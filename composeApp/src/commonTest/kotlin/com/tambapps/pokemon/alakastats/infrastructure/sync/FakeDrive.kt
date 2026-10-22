package com.tambapps.pokemon.alakastats.infrastructure.sync

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudError
import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudStorage
import com.tambapps.pokemon.alakastats.infrastructure.cloud.FileRevision
import com.tambapps.pokemon.alakastats.infrastructure.cloud.Member
import com.tambapps.pokemon.alakastats.infrastructure.cloud.ShareRole
import com.tambapps.pokemon.alakastats.infrastructure.cloud.SharedFile
import com.tambapps.pokemon.alakastats.infrastructure.cloud.SharingCloudStorage
import kotlin.time.Clock

/**
 * A Drive with several users: each has a private app folder, and visible files can be shared between them.
 * As with drive.file, a user reaches a shared file only once the app created it for them or they picked it.
 */
class FakeDrive(private val clock: Clock) {

    class Revision(val id: String, val by: String, val bytes: ByteArray)

    class File(
        val id: String,
        var name: String,
        var properties: Map<String, String>,
        val owner: String,
        val isFolder: Boolean = false,
        var trashed: Boolean = false,
        var writersCanShare: Boolean = true,
        val members: MutableMap<String, ShareRole> = mutableMapOf(),
        val revisions: MutableList<Revision> = mutableListOf(),
    ) {
        val bytes get() = revisions.last().bytes
    }

    val files = linkedMapOf<String, File>()
    private val appData = mutableMapOf<String, FakeCloudStorage>()
    private val grants = mutableMapOf<String, MutableSet<String>>()
    private var nextId = 0

    /** run right before a user's upload to an existing file: e.g. someone else writes it meanwhile */
    var beforeUpload: (suspend (fileId: String) -> Unit)? = null

    fun client(user: String, sharingConsent: Boolean = true) = Client(user, sharingConsent)

    /** the user picks the file in the Picker */
    fun pick(user: String, fileId: String) {
        grants.getOrPut(user) { mutableSetOf() }.add(fileId)
    }

    /** another app (or device) of [user] writes the file */
    fun writeAs(user: String, fileId: String, bytes: ByteArray) {
        val file = files.getValue(fileId)
        file.revisions.add(Revision("rev-${nextId++}", user, bytes))
    }

    fun appDataOf(user: String) = appData.getOrPut(user) { FakeCloudStorage(clock, email = user) }

    private fun newId(prefix: String) = "$prefix-${nextId++}"

    inner class Client(private val user: String, private val sharingConsent: Boolean) :
        SharingCloudStorage, CloudStorage by appDataOf(user) {

        private fun reachable(fileId: String): Either<CloudError, File> {
            if (!sharingConsent) return CloudError.NeedsUserInteraction().left()
            val file = files[fileId] ?: return CloudError.NotFound("No file $fileId").left()
            val isMember = file.owner == user || user in file.members
            val granted = fileId in grants[user].orEmpty()
            if (!isMember || !granted) return CloudError.NotFound("File not found: $fileId").left()
            // a trashed file is gone for the others
            if (file.trashed && file.owner != user) return CloudError.NotFound("File not found: $fileId").left()
            return file.right()
        }

        private fun File.role() = if (owner == user) ShareRole.OWNER else members.getValue(user)

        private fun File.toShared() = SharedFile(
            id = id,
            name = name,
            version = revisions.size.toLong(),
            headRevisionId = revisions.lastOrNull()?.id,
            modifiedAt = clock.now(),
            properties = properties,
            trashed = trashed,
            canEdit = role() != ShareRole.VIEWER,
            canShare = role() == ShareRole.OWNER || (role() == ShareRole.EDITOR && writersCanShare),
            ownedByMe = owner == user,
            ownerEmail = owner,
        )

        override suspend fun ensureFolder(name: String): Either<CloudError, String> {
            if (!sharingConsent) return CloudError.NeedsUserInteraction().left()
            files.values.firstOrNull { it.isFolder && it.owner == user && it.name == name }?.let { return it.id.right() }
            val folder = File(newId("folder"), name, emptyMap(), owner = user, isFolder = true)
            files[folder.id] = folder
            pick(user, folder.id)
            return folder.id.right()
        }

        override suspend fun getSharedFile(fileId: String) = reachable(fileId).map { it.toShared() }

        override suspend fun downloadSharedFile(fileId: String) = reachable(fileId).map { it.bytes }

        override suspend fun uploadSharedFile(
            name: String,
            bytes: ByteArray,
            properties: Map<String, String>,
            existingFileId: String?,
            folderId: String?,
        ): Either<CloudError, SharedFile> {
            if (existingFileId == null) {
                if (!sharingConsent) return CloudError.NeedsUserInteraction().left()
                val file = File(newId("shared"), name, properties, owner = user)
                file.revisions.add(Revision(newId("rev"), user, bytes))
                files[file.id] = file
                pick(user, file.id)
                return file.toShared().right()
            }
            beforeUpload?.invoke(existingFileId)
            val file = reachable(existingFileId).fold({ return it.left() }, { it })
            if (file.role() == ShareRole.VIEWER) return CloudError.FileAccessDenied("Read only").left()
            file.name = name
            file.properties = properties
            file.revisions.add(Revision(newId("rev"), user, bytes))
            return file.toShared().right()
        }

        override suspend fun trashSharedFile(fileId: String): Either<CloudError, Unit> {
            val file = reachable(fileId).fold({ return it.left() }, { it })
            if (file.owner != user) return CloudError.FileAccessDenied("Only the owner can delete").left()
            file.trashed = true
            return Unit.right()
        }

        override suspend fun share(fileId: String, email: String, role: ShareRole, message: String?): Either<CloudError, Unit> {
            val file = reachable(fileId).fold({ return it.left() }, { it })
            if (!file.toShared().canShare) return CloudError.FileAccessDenied("Can't share").left()
            file.members[email] = role
            return Unit.right()
        }

        override suspend fun listMembers(fileId: String) = reachable(fileId).map { file ->
            listOf(Member(file.owner, file.owner, null, ShareRole.OWNER)) +
                    file.members.map { (email, role) -> Member(email, email, null, role) }
        }

        override suspend fun setMemberRole(fileId: String, permissionId: String, role: ShareRole) =
            reachable(fileId).map { it.members[permissionId] = role }

        override suspend fun removeMember(fileId: String, permissionId: String) =
            reachable(fileId).map { it.members.remove(permissionId); Unit }

        override suspend fun setEditorsCanShare(fileId: String, canShare: Boolean) =
            reachable(fileId).map { it.writersCanShare = canShare }

        override suspend fun listRevisions(fileId: String) = reachable(fileId).map { file ->
            file.revisions.map { FileRevision(it.id, null, byMe = it.by == user) }
        }

        override suspend fun downloadRevision(fileId: String, revisionId: String) = reachable(fileId).map { file ->
            file.revisions.single { it.id == revisionId }.bytes
        }
    }
}
