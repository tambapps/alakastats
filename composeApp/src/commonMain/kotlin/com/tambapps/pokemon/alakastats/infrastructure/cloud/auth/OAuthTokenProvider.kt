package com.tambapps.pokemon.alakastats.infrastructure.cloud.auth

import arrow.core.Either
import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudError

const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"

/**
 * Gives OAuth access tokens. The only platform-specific part of the cloud sync.
 */
interface OAuthTokenProvider {

    /**
     * @param interactive whether a UI can be shown to the user (consent screen, account picker).
     * When false and a UI would be needed, returns [CloudError.NeedsUserInteraction]
     */
    suspend fun accessToken(interactive: Boolean): Either<CloudError, String>

    /**
     * Forgets the given token, e.g. after a 401, so that the next [accessToken] call gets a fresh one
     */
    suspend fun invalidate(token: String)

    /**
     * Revokes the access and forgets every stored token
     */
    suspend fun signOut()
}
