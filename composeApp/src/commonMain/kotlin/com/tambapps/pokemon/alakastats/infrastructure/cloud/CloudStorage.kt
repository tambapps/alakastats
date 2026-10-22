package com.tambapps.pokemon.alakastats.infrastructure.cloud

import arrow.core.Either
import kotlin.time.Instant

/**
 * A file stored in a cloud provider's app-private space.
 * @param properties small key/values stored with the file, readable when listing without downloading the content
 */
data class CloudFile(
    val id: String,
    val name: String,
    val modifiedAt: Instant?,
    val properties: Map<String, String>,
)

sealed interface CloudError {
    val message: String

    /** No connection, timeout... the local data stays the source of truth until the next sync */
    data class Network(override val message: String, val cause: Throwable? = null) : CloudError

    /** The user's storage quota is exceeded */
    data class StorageFull(override val message: String = "Your cloud storage is full") : CloudError

    /** The user revoked the access to the app, or the grant expired. A new interactive connection is needed */
    data class AccessRevoked(override val message: String = "The access to your cloud storage was revoked") : CloudError

    /** A token can't be obtained without showing a UI to the user */
    data class NeedsUserInteraction(override val message: String = "Please reconnect to your cloud storage") : CloudError

    /** The user cancelled the interactive connection */
    data class Cancelled(override val message: String = "Connection cancelled") : CloudError

    /** Still rate limited after all the retries */
    data class RateLimited(override val message: String = "Too many requests, try again later") : CloudError

    data class NotFound(override val message: String) : CloudError

    /** No access to a given file, e.g. a shared file whose owner removed the user. The other files still work */
    data class FileAccessDenied(override val message: String = "No access to this file") : CloudError

    data class Unknown(override val message: String, val cause: Throwable? = null) : CloudError
}

/**
 * Storage of files in a cloud provider. Implementations: Google Drive for now, Dropbox/OneDrive/WebDAV could follow.
 */
interface CloudStorage {

    suspend fun list(): Either<CloudError, List<CloudFile>>

    suspend fun download(fileId: String): Either<CloudError, ByteArray>

    /**
     * Creates the file, or replaces its content and properties when [existingFileId] is given
     */
    suspend fun upload(
        name: String,
        bytes: ByteArray,
        properties: Map<String, String>,
        existingFileId: String? = null,
    ): Either<CloudError, CloudFile>

    suspend fun delete(fileId: String): Either<CloudError, Unit>

    /**
     * @return the e-mail of the connected account, to show the user which one it is
     */
    suspend fun accountEmail(): Either<CloudError, String?>
}
