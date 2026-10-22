package com.tambapps.pokemon.alakastats.infrastructure.cloud

import android.app.Activity
import android.content.Context
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.tambapps.pokemon.alakastats.infrastructure.cloud.auth.GoogleOAuthClient
import com.tambapps.pokemon.alakastats.infrastructure.cloud.auth.OAuthTokenProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Google Identity Services' Authorization API. It gives no refresh token without a backend: an expired
 * token is replaced by calling authorize() again, which needs no UI as long as the user didn't revoke the access.
 */
class GoogleAuthorizationTokenProvider(
    private val context: Context,
    private val bridge: AndroidAuthorizationBridge,
    private val oAuthClient: GoogleOAuthClient,
) : OAuthTokenProvider {

    private val client get() = Identity.getAuthorizationClient(context)
    private val mutex = Mutex()
    private val cachedTokens = mutableMapOf<Set<String>, String>()

    override suspend fun accessToken(interactive: Boolean, scopes: Set<String>): Either<CloudError, String> = mutex.withLock {
        cachedTokens[scopes]?.let { return it.right() }
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(scopes.map(::Scope))
            .build()
        val result = try {
            client.authorize(request).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return e.toCloudError().left()
        }
        val token = if (result.hasResolution()) {
            if (!interactive) return CloudError.NeedsUserInteraction().left()
            resolve(result).getOrElse { return it.left() }
        } else result.accessToken ?: return CloudError.Unknown("Google gave no access token").left()
        cachedTokens[scopes] = token
        token.right()
    }

    private suspend fun resolve(result: AuthorizationResult): Either<CloudError, String> {
        val pendingIntent = result.pendingIntent ?: return CloudError.Unknown("Google gave no consent screen").left()
        val activityResult = bridge.launch(pendingIntent) ?: return CloudError.NeedsUserInteraction().left()
        if (activityResult.resultCode != Activity.RESULT_OK) return CloudError.Cancelled().left()
        return try {
            client.getAuthorizationResultFromIntent(activityResult.data).accessToken?.right()
                ?: CloudError.Unknown("Google gave no access token").left()
        } catch (e: ApiException) {
            e.toCloudError().left()
        }
    }

    override suspend fun invalidate(token: String) = mutex.withLock {
        cachedTokens.values.removeAll { it == token }
        clearToken(token)
    }

    override suspend fun signOut() {
        val tokens = mutex.withLock { cachedTokens.values.toSet().also { cachedTokens.clear() } }
            .ifEmpty { setOfNotNull(accessToken(interactive = false).getOrNull()) }
        // revoking a token revokes the whole grant, every scope included
        tokens.firstOrNull()?.let { oAuthClient.revoke(it) }
        tokens.forEach { clearToken(it) }
        mutex.withLock { cachedTokens.clear() }
    }

    private suspend fun clearToken(token: String) {
        try {
            client.clearToken(ClearTokenRequest.builder().setToken(token).build()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    private fun Exception.toCloudError(): CloudError =
        if (this is ApiException && statusCode == CommonStatusCodes.NETWORK_ERROR) CloudError.Network(message ?: "Network error", this)
        else CloudError.Unknown(message ?: "Google authorization failed", this)
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
