package com.tambapps.pokemon.alakastats.infrastructure.cloud

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.tambapps.pokemon.alakastats.infrastructure.cloud.auth.PRIVATE_SYNC_SCOPES
import com.tambapps.pokemon.alakastats.infrastructure.cloud.auth.GoogleOAuthClient
import com.tambapps.pokemon.alakastats.infrastructure.cloud.auth.GoogleTokenResponse
import com.tambapps.pokemon.alakastats.infrastructure.cloud.auth.OAuthTokenProvider
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import platform.AuthenticationServices.ASPresentationAnchor
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.AuthenticationServices.ASWebAuthenticationSessionErrorCodeCanceledLogin
import platform.AuthenticationServices.ASWebAuthenticationSessionErrorDomain
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSBundle
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.io.encoding.Base64
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * OAuth 2.0 authorization code flow with PKCE, through ASWebAuthenticationSession.
 * The refresh token is kept in the Keychain, the access token only in memory.
 */
class IosPkceTokenProvider(
    private val oAuthClient: GoogleOAuthClient,
    private val keychain: KeychainStore = KeychainStore("com.tambapps.pokemon.alakastats.googledrive"),
    private val clock: Clock = Clock.System,
) : OAuthTokenProvider {

    private val mutex = Mutex()
    private var accessToken: String? = null
    private var expiresAt: Instant = Instant.DISTANT_PAST

    // strong references, so that the session isn't collected while it's shown
    private var session: ASWebAuthenticationSession? = null
    private var contextProvider: PresentationContextProvider? = null

    override suspend fun accessToken(interactive: Boolean, scopes: Set<String>): Either<CloudError, String> = mutex.withLock {
        val refreshToken = keychain.get(REFRESH_TOKEN_KEY)
        // the tokens give access to every scope granted so far
        val hasScopes = refreshToken != null && grantedScopes().containsAll(scopes)
        val current = accessToken
        if (hasScopes && current != null && clock.now() < expiresAt - EXPIRY_MARGIN) return current.right()
        val clientId = clientId() ?: return CloudError.Unknown("Google Drive isn't configured in this build").left()

        if (refreshToken != null && hasScopes) {
            when (val refreshed = oAuthClient.refresh(clientId, refreshToken)) {
                is Either.Right -> return store(refreshed.value)
                is Either.Left -> when (refreshed.value) {
                    // the user revoked the access: the refresh token is useless now
                    is CloudError.AccessRevoked -> keychain.delete(REFRESH_TOKEN_KEY)
                    else -> return refreshed.value.left()
                }
            }
            if (!interactive) return CloudError.AccessRevoked().left()
        }
        if (!interactive) return CloudError.NeedsUserInteraction().left()
        // the scopes already granted are kept: the new refresh token covers them all
        val response = authorizeInteractively(clientId, scopes + grantedScopes()).getOrElse { return it.left() }
        store(response)
    }

    private fun grantedScopes(): Set<String> {
        val stored = keychain.get(GRANTED_SCOPES_KEY)
            ?: return if (keychain.get(REFRESH_TOKEN_KEY) != null) PRIVATE_SYNC_SCOPES else emptySet()
        return stored.split(' ').filter { it.isNotBlank() }.toSet()
    }

    override suspend fun invalidate(token: String) = mutex.withLock {
        if (accessToken == token) {
            accessToken = null
            expiresAt = Instant.DISTANT_PAST
        }
    }

    override suspend fun signOut() = mutex.withLock {
        val token = keychain.get(REFRESH_TOKEN_KEY) ?: accessToken
        keychain.delete(REFRESH_TOKEN_KEY)
        keychain.delete(GRANTED_SCOPES_KEY)
        accessToken = null
        expiresAt = Instant.DISTANT_PAST
        token?.let { oAuthClient.revoke(it) }
        Unit
    }

    private fun store(response: GoogleTokenResponse): Either<CloudError, String> {
        response.refreshToken?.let { keychain.set(REFRESH_TOKEN_KEY, it) }
        response.scope?.let { keychain.set(GRANTED_SCOPES_KEY, it) }
        accessToken = response.accessToken
        expiresAt = clock.now() + response.expiresInSeconds.seconds
        return response.accessToken.right()
    }

    private suspend fun authorizeInteractively(clientId: String, scopes: Set<String>): Either<CloudError, GoogleTokenResponse> {
        val codeVerifier = base64Url(secureRandomBytes(32))
        val codeChallenge = base64Url(sha256(codeVerifier.encodeToByteArray()))
        val state = base64Url(secureRandomBytes(16))
        val scheme = "com.googleusercontent.apps." + clientId.removeSuffix(".apps.googleusercontent.com")
        val redirectUri = "$scheme:/oauthredirect"
        val authUrl = URLBuilder(AUTH_URL).apply {
            parameters.append("client_id", clientId)
            parameters.append("redirect_uri", redirectUri)
            parameters.append("response_type", "code")
            parameters.append("scope", scopes.joinToString(" "))
            parameters.append("include_granted_scopes", "true")
            parameters.append("code_challenge", codeChallenge)
            parameters.append("code_challenge_method", "S256")
            parameters.append("state", state)
        }.buildString()

        val callbackUrl = presentSession(authUrl, scheme).getOrElse { return it.left() }
        val parameters = Url(callbackUrl).parameters
        if (parameters["state"] != state) return CloudError.Unknown("Invalid authorization response").left()
        parameters["error"]?.let { error ->
            return if (error == "access_denied") CloudError.Cancelled().left()
            else CloudError.Unknown("Google authorization failed: $error").left()
        }
        val code = parameters["code"] ?: return CloudError.Unknown("Google gave no authorization code").left()
        return oAuthClient.exchangeCode(clientId, code, codeVerifier, redirectUri)
    }

    private suspend fun presentSession(authUrl: String, callbackScheme: String): Either<CloudError, String> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val provider = PresentationContextProvider()
            val session = ASWebAuthenticationSession(
                uRL = NSURL(string = authUrl),
                callbackURLScheme = callbackScheme,
                completionHandler = { url: NSURL?, error: NSError? ->
                    this@IosPkceTokenProvider.session = null
                    contextProvider = null
                    val result = when {
                        url?.absoluteString != null -> url.absoluteString!!.right()
                        error != null && error.domain == ASWebAuthenticationSessionErrorDomain
                                && error.code == ASWebAuthenticationSessionErrorCodeCanceledLogin -> CloudError.Cancelled().left()
                        else -> CloudError.Unknown(error?.localizedDescription ?: "Google authorization failed").left()
                    }
                    if (continuation.isActive) continuation.resume(result)
                }
            )
            session.presentationContextProvider = provider
            session.prefersEphemeralWebBrowserSession = false
            this@IosPkceTokenProvider.session = session
            contextProvider = provider
            continuation.invokeOnCancellation { session.cancel() }
            if (!session.start()) {
                continuation.resume(CloudError.Unknown("Couldn't open Google's sign in page").left())
            }
        }
    }

    private fun clientId(): String? =
        (NSBundle.mainBundle.objectForInfoDictionaryKey(CLIENT_ID_INFO_KEY) as? String)
            ?.takeIf { it.isNotBlank() && !it.startsWith("$(") }

    private class PresentationContextProvider : NSObject(), ASWebAuthenticationPresentationContextProvidingProtocol {
        override fun presentationAnchorForWebAuthenticationSession(session: ASWebAuthenticationSession): ASPresentationAnchor {
            return UIApplication.sharedApplication.connectedScenes
                .filterIsInstance<UIWindowScene>()
                .flatMap { scene -> scene.windows.filterIsInstance<UIWindow>() }
                .let { windows -> windows.firstOrNull { it.isKeyWindow() } ?: windows.firstOrNull() }
                ?: UIWindow()
        }
    }

    private companion object {
        const val AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
        const val CLIENT_ID_INFO_KEY = "GoogleIosClientId"
        const val REFRESH_TOKEN_KEY = "refresh_token"
        // space separated, as Google gives them
        const val GRANTED_SCOPES_KEY = "granted_scopes"
        val EXPIRY_MARGIN = 60.seconds

        fun base64Url(bytes: ByteArray) = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(bytes)

        @OptIn(ExperimentalForeignApi::class)
        fun secureRandomBytes(size: Int): ByteArray {
            val bytes = ByteArray(size)
            val status = bytes.usePinned { SecRandomCopyBytes(kSecRandomDefault, size.convert(), it.addressOf(0)) }
            check(status == errSecSuccess) { "Couldn't generate random bytes" }
            return bytes
        }

        @OptIn(ExperimentalForeignApi::class, ExperimentalUnsignedTypes::class)
        fun sha256(input: ByteArray): ByteArray {
            val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
            input.usePinned { inputPin ->
                digest.usePinned { digestPin ->
                    CC_SHA256(inputPin.addressOf(0), input.size.convert(), digestPin.addressOf(0))
                }
            }
            return digest.toByteArray()
        }
    }
}
