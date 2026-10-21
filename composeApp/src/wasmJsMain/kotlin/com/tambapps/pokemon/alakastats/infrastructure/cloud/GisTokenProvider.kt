package com.tambapps.pokemon.alakastats.infrastructure.cloud

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tambapps.pokemon.alakastats.infrastructure.cloud.auth.DRIVE_APPDATA_SCOPE
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

    override suspend fun accessToken(interactive: Boolean): Either<CloudError, String> = mutex.withLock {
        storedToken()?.let { return it.right() }
        if (!interactive) return CloudError.NeedsUserInteraction().left()

        if (!loadGis().await<JsBoolean>().toBoolean()) {
            return CloudError.Network("Couldn't load Google's sign in. Are you offline?").left()
        }
        val response = requestToken(clientId, DRIVE_APPDATA_SCOPE).await<JsAny>()
        errorOf(response)?.let { error ->
            return when (error) {
                "popup_closed", "access_denied" -> CloudError.Cancelled()
                "popup_failed_to_open" -> CloudError.Unknown("Google's sign in popup was blocked. Please allow popups for this site")
                else -> CloudError.Unknown("Google authorization failed: $error")
            }.left()
        }
        val token = accessTokenOf(response) ?: return CloudError.Unknown("Google gave no access token").left()
        store(token, clock.now() + expiresInOf(response).seconds)
        token.right()
    }

    override suspend fun invalidate(token: String) = mutex.withLock {
        if (window.sessionStorage.getItem(TOKEN_KEY) == token) clear()
    }

    override suspend fun signOut() = mutex.withLock {
        val token = window.sessionStorage.getItem(TOKEN_KEY)
        clear()
        if (token != null && loadGis().await<JsBoolean>().toBoolean()) revoke(token)
    }

    private fun storedToken(): String? {
        val token = window.sessionStorage.getItem(TOKEN_KEY) ?: return null
        val expiresAt = window.sessionStorage.getItem(EXPIRES_AT_KEY)?.toLongOrNull()
            ?.let { Instant.fromEpochMilliseconds(it) } ?: return null
        return token.takeIf { clock.now() < expiresAt - EXPIRY_MARGIN }
    }

    private fun store(token: String, expiresAt: Instant) {
        window.sessionStorage.setItem(TOKEN_KEY, token)
        window.sessionStorage.setItem(EXPIRES_AT_KEY, expiresAt.toEpochMilliseconds().toString())
    }

    private fun clear() {
        window.sessionStorage.removeItem(TOKEN_KEY)
        window.sessionStorage.removeItem(EXPIRES_AT_KEY)
    }

    private companion object {
        const val TOKEN_KEY = "alakastats.googledrive.token"
        const val EXPIRES_AT_KEY = "alakastats.googledrive.expiresAt"
        val EXPIRY_MARGIN = 60.seconds
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
