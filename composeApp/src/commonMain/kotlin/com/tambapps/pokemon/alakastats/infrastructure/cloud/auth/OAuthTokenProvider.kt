package com.tambapps.pokemon.alakastats.infrastructure.cloud.auth

import arrow.core.Either
import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudError

const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
/** the files the app created, or that the user picked with the app: for shared teams */
const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"

/** the private sync, in the app's hidden folder */
val PRIVATE_SYNC_SCOPES = setOf(DRIVE_APPDATA_SCOPE)
/** the private sync and the shared teams */
val SHARING_SCOPES = setOf(DRIVE_APPDATA_SCOPE, DRIVE_FILE_SCOPE)

/**
 * Gives OAuth access tokens. The only platform-specific part of the cloud sync.
 */
interface OAuthTokenProvider {

    /**
     * @param interactive whether a UI can be shown to the user (consent screen, account picker).
     * When false and a UI would be needed, returns [CloudError.NeedsUserInteraction]
     * @param scopes the permissions the token needs. Asking more than already granted needs the user's consent
     */
    suspend fun accessToken(interactive: Boolean, scopes: Set<String> = PRIVATE_SYNC_SCOPES): Either<CloudError, String>

    /**
     * Forgets the given token, e.g. after a 401, so that the next [accessToken] call gets a fresh one
     */
    suspend fun invalidate(token: String)

    /**
     * Revokes the access and forgets every stored token
     */
    suspend fun signOut()
}
