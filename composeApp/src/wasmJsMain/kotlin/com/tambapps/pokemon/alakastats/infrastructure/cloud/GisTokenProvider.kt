package com.tambapps.pokemon.alakastats.infrastructure.cloud

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tambapps.pokemon.alakastats.infrastructure.cloud.auth.PRIVATE_SYNC_SCOPES
import com.tambapps.pokemon.alakastats.infrastructure.cloud.auth.SHARING_SCOPES
import com.tambapps.pokemon.alakastats.infrastructure.cloud.auth.OAuthTokenProvider
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.js.Promise
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Google Identity Services' token client. Without a backend, the browser gets no refresh token: an access token
 * lasts about an hour, and getting a new one opens Google's popup, which browsers only allow after a user click.
 * The token is kept in the session storage, so that reloading the page doesn't ask to reconnect.
 */
class GisTokenProvider(
    private val clientId: String,
    private val clock: Clock = Clock.System,
) : OAuthTokenProvider {

    private val mutex = Mutex()

    override suspend fun accessToken(interactive: Boolean, scopes: Set<String>): Either<CloudError, String> = mutex.withLock {
        storedToken(scopes)?.let { return it.right() }
        if (!interactive) return CloudError.NeedsUserInteraction().left()

        if (!loadGis().await<JsBoolean>().toBoolean()) {
            return CloudError.Network("Couldn't load Google's sign in. Are you offline?").left()
        }
        val response = requestToken(clientId, scopes.joinToString(" ")).await<JsAny>()
        errorOf(response)?.let { error ->
            return when (error) {
                "popup_closed", "access_denied" -> CloudError.Cancelled()
                "popup_failed_to_open" -> CloudError.Unknown("Google's sign in popup was blocked. Please allow popups for this site")
                else -> CloudError.Unknown("Google authorization failed: $error")
            }.left()
        }
        val token = accessTokenOf(response) ?: return CloudError.Unknown("Google gave no access token").left()
        store(scopes, token, clock.now() + expiresInOf(response).seconds)
        token.right()
    }

    override suspend fun invalidate(token: String) = mutex.withLock {
        KNOWN_SCOPES.filter { window.sessionStorage.getItem(tokenKey(it)) == token }.forEach(::clear)
    }

    override suspend fun signOut() = mutex.withLock {
        val token = KNOWN_SCOPES.firstNotNullOfOrNull { window.sessionStorage.getItem(tokenKey(it)) }
        KNOWN_SCOPES.forEach(::clear)
        // revoking a token revokes the whole grant, every scope included
        if (token != null && loadGis().await<JsBoolean>().toBoolean()) revoke(token)
    }

    /**
     * A token with more scopes than asked works too
     */
    private fun storedToken(scopes: Set<String>): String? = (listOf(scopes) + KNOWN_SCOPES)
        .filter { it.containsAll(scopes) }
        .firstNotNullOfOrNull { candidate ->
            val token = window.sessionStorage.getItem(tokenKey(candidate)) ?: return@firstNotNullOfOrNull null
            val expiresAt = window.sessionStorage.getItem(expiresAtKey(candidate))?.toLongOrNull()
                ?.let { Instant.fromEpochMilliseconds(it) } ?: return@firstNotNullOfOrNull null
            token.takeIf { clock.now() < expiresAt - EXPIRY_MARGIN }
        }

    private fun store(scopes: Set<String>, token: String, expiresAt: Instant) {
        window.sessionStorage.setItem(tokenKey(scopes), token)
        window.sessionStorage.setItem(expiresAtKey(scopes), expiresAt.toEpochMilliseconds().toString())
    }

    private fun clear(scopes: Set<String>) {
        window.sessionStorage.removeItem(tokenKey(scopes))
        window.sessionStorage.removeItem(expiresAtKey(scopes))
    }

    private companion object {
        val KNOWN_SCOPES = listOf(PRIVATE_SYNC_SCOPES, SHARING_SCOPES)
        val EXPIRY_MARGIN = 60.seconds

        // e.g. alakastats.googledrive.drive.appdata+drive.file.token
        fun keyPrefix(scopes: Set<String>) = "alakastats.googledrive." + scopes.sorted().joinToString("+") { it.substringAfterLast('/') }
        fun tokenKey(scopes: Set<String>) = keyPrefix(scopes) + ".token"
        fun expiresAtKey(scopes: Set<String>) = keyPrefix(scopes) + ".expiresAt"
    }
}

/**
 * Loads Google Identity Services' script, once. Resolves to false if it couldn't be loaded
 */
private fun loadGis(): Promise<JsBoolean> = js("""
    new Promise((resolve) => {
        if (window.google && window.google.accounts && window.google.accounts.oauth2) {
            resolve(true);
            return;
        }
        const script = document.createElement('script');
        script.src = 'https://accounts.google.com/gsi/client';
        script.async = true;
        script.onload = () => resolve(true);
        script.onerror = () => resolve(false);
        document.head.appendChild(script);
    })
""")

/**
 * Opens Google's popup. Never rejects: resolves to the token response, or to an object with an error
 */
private fun requestToken(clientId: String, scope: String): Promise<JsAny> = js("""
    new Promise((resolve) => {
        const client = google.accounts.oauth2.initTokenClient({
            client_id: clientId,
            scope: scope,
            // no consent screen if the user already granted the access
            prompt: '',
            callback: (response) => resolve(response),
            error_callback: (error) => resolve({ error: (error && error.type) || 'unknown' }),
        });
        client.requestAccessToken();
    })
""")

private fun revoke(token: String): Unit = js("""
    { if (window.google && window.google.accounts && window.google.accounts.oauth2) google.accounts.oauth2.revoke(token, () => {}); }
""")

private fun errorOf(response: JsAny): String? = js("response.error || null")

private fun accessTokenOf(response: JsAny): String? = js("response.access_token || null")

private fun expiresInOf(response: JsAny): Int = js("Number(response.expires_in) || 3600")
