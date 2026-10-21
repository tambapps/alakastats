package com.tambapps.pokemon.alakastats.infrastructure.cloud.auth

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudError
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GoogleTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresInSeconds: Long = 3600,
    @SerialName("refresh_token") val refreshToken: String? = null,
)

@Serializable
private data class GoogleTokenErrorResponse(
    val error: String? = null,
    @SerialName("error_description") val description: String? = null,
)

/**
 * Calls of Google's OAuth endpoints for installed apps (no client secret, PKCE)
 */
class GoogleOAuthClient(
    private val httpClient: HttpClient,
    private val json: Json,
) {

    suspend fun exchangeCode(clientId: String, code: String, codeVerifier: String, redirectUri: String) = tokenRequest(
        "client_id" to clientId,
        "code" to code,
        "code_verifier" to codeVerifier,
        "redirect_uri" to redirectUri,
        "grant_type" to "authorization_code",
    )

    suspend fun refresh(clientId: String, refreshToken: String) = tokenRequest(
        "client_id" to clientId,
        "refresh_token" to refreshToken,
        "grant_type" to "refresh_token",
    )

    /**
     * Best effort: revoking a refresh token also revokes the access tokens obtained with it
     */
    suspend fun revoke(token: String) {
        try {
            httpClient.submitForm(REVOKE_URL, parameters { append("token", token) })
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    private suspend fun tokenRequest(vararg params: Pair<String, String>): Either<CloudError, GoogleTokenResponse> = try {
        val response = httpClient.submitForm(TOKEN_URL, parameters { params.forEach { (k, v) -> append(k, v) } })
        val body = response.bodyAsText()
        if (response.status.isSuccess()) json.decodeFromString<GoogleTokenResponse>(body).right()
        else {
            val error = runCatching { json.decodeFromString<GoogleTokenErrorResponse>(body) }.getOrNull()
            when (error?.error) {
                // the refresh token was revoked by the user, or expired
                "invalid_grant" -> CloudError.AccessRevoked().left()
                else -> CloudError.Unknown(error?.description ?: "Google authentication failed (${response.status.value})").left()
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        CloudError.Network(e.message ?: "Network error", e).left()
    } catch (e: Exception) {
        CloudError.Unknown(e.message ?: "Google authentication failed", e).left()
    }

    private companion object {
        const val TOKEN_URL = "https://oauth2.googleapis.com/token"
        const val REVOKE_URL = "https://oauth2.googleapis.com/revoke"
    }
}
