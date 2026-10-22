package com.tambapps.pokemon.alakastats.infrastructure.cloud

import arrow.core.Either
import kotlin.time.Instant

enum class ShareRole { OWNER, EDITOR, VIEWER }

/**
 * A file the user can see in their cloud storage (not the app's hidden folder), possibly shared with other people
 * @param version increases with each change of the file
 * @param headRevisionId the id of the current content's revision
 */
data class SharedFile(
    val id: String,
    val name: String,
    val version: Long,
    val headRevisionId: String?,
    val modifiedAt: Instant?,
    val properties: Map<String, String>,
    val trashed: Boolean,
    val canEdit: Boolean,
    val canShare: Boolean,
    val ownedByMe: Boolean,
    val ownerEmail: String?,
) {
    val role get() = when {
        ownedByMe -> ShareRole.OWNER
        canEdit -> ShareRole.EDITOR
        else -> ShareRole.VIEWER
    }
}

data class Member(val permissionId: String, val email: String?, val name: String?, val role: ShareRole)

/**
 * @param byMe whether the current user wrote this revision
 */
data class FileRevision(val id: String, val modifiedAt: Instant?, val byMe: Boolean)

/**
 * Files shared between several users. Only the files the app created, or that the user picked with the app, are
 * reachable.
 */
interface SharingCloudStorage : CloudStorage {

    /**
     * @return the id of the folder of this name created by the app, created if needed
     */
    suspend fun ensureFolder(name: String): Either<CloudError, String>

    suspend fun getSharedFile(fileId: String): Either<CloudError, SharedFile>

    suspend fun downloadSharedFile(fileId: String): Either<CloudError, ByteArray>

    /**
     * Creates the file in [folderId], or replaces the content of [existingFileId]
     */
    suspend fun uploadSharedFile(
        name: String,
        bytes: ByteArray,
        properties: Map<String, String>,
        existingFileId: String?,
        folderId: String? = null,
    ): Either<CloudError, SharedFile>

    /** Moves the file to the owner's trash: nobody can access it anymore */
    suspend fun trashSharedFile(fileId: String): Either<CloudError, Unit>

    /**
     * @param message added to the invitation e-mail
     */
    suspend fun share(fileId: String, email: String, role: ShareRole, message: String?): Either<CloudError, Unit>

    suspend fun listMembers(fileId: String): Either<CloudError, List<Member>>

    suspend fun setMemberRole(fileId: String, permissionId: String, role: ShareRole): Either<CloudError, Unit>

    suspend fun removeMember(fileId: String, permissionId: String): Either<CloudError, Unit>

    /** Whether editors can invite other people */
    suspend fun setEditorsCanShare(fileId: String, canShare: Boolean): Either<CloudError, Unit>

    /** The content revisions, oldest first */
    suspend fun listRevisions(fileId: String): Either<CloudError, List<FileRevision>>

    suspend fun downloadRevision(fileId: String, revisionId: String): Either<CloudError, ByteArray>
}
