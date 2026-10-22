package com.tambapps.pokemon.alakastats.infrastructure.sync

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudError
import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudFile
import com.tambapps.pokemon.alakastats.infrastructure.cloud.Member
import com.tambapps.pokemon.alakastats.infrastructure.cloud.ShareRole
import com.tambapps.pokemon.alakastats.infrastructure.cloud.SharedFile
import com.tambapps.pokemon.alakastats.infrastructure.cloud.SharingCloudStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * What syncing a team did
 * @param changedLocally whether the local team was written or deleted
 * @param merge set if the team was merged
 * @param notice something to tell the user
 */
internal data class TeamSyncOutcome(
    val changedLocally: Boolean = false,
    val merge: TeamMerge? = null,
    val notice: String? = null,
)

internal interface SyncStateAccess {
    suspend fun current(): SyncState
    suspend fun update(transform: (SyncState) -> SyncState): SyncState
}

/**
 * The shared teams: visible files of the owner's cloud storage, that the collaborators picked. Each device of a user
 * knows which teams are shared through the [SharedTeamsIndex], stored in the app's hidden folder.
 */
internal class SharedTeamSync(
    private val storage: SharingCloudStorage,
    private val local: LocalTeams,
    private val state: SyncStateAccess,
    private val json: Json,
    private val clock: Clock,
) {

    private var indexFileId: String? = null
    // the index as the cloud has it
    private var pushedIndex: SharedTeamsIndex? = null

    /**
     * Merges the index of the cloud with the local one, and uploads it if the cloud's misses something
     * @param remoteFiles the files of the app's hidden folder
     */
    suspend fun syncIndex(remoteFiles: List<CloudFile>): Either<CloudError, SharedTeamsIndex> {
        val indexFile = remoteFiles.firstOrNull { it.properties[PROPERTY_KIND] == INDEX_KIND }
        indexFileId = indexFile?.id
        val remoteIndex = indexFile?.let { file ->
            val bytes = storage.download(file.id).getOrElse { return it.left() }
            // unreadable: rebuilt from the local one
            runCatching { json.decodeFromString<SharedTeamsIndex>(bytes.decodeToString()) }.getOrNull()
        } ?: SharedTeamsIndex()
        pushedIndex = remoteIndex
        val merged = state.current().sharedIndex.merge(remoteIndex)
        state.update { it.copy(sharedIndex = merged) }
        pushIndex().getOrElse { return it.left() }
        return merged.right()
    }

    /**
     * Uploads the local index, if it changed since the cloud's was read, e.g. when a team lost its access
     */
    suspend fun pushIndex(): Either<CloudError, Unit> {
        val index = state.current().sharedIndex
        if (index == pushedIndex) return Unit.right()
        val file = storage.upload(INDEX_FILE_NAME, json.encodeToString(index).encodeToByteArray(), INDEX_PROPERTIES, indexFileId)
            .getOrElse { return it.left() }
        indexFileId = file.id
        pushedIndex = index
        return Unit.right()
    }

    /**
     * Trashes the files of the teams deleted or no longer shared, left to do e.g. by an offline device
     */
    suspend fun trashPending(): Either<CloudError, Unit> {
        for (fileId in state.current().pendingSharedTrash) {
            val error = storage.trashSharedFile(fileId).leftOrNull()
            // already gone: nothing left to do
            if (error != null && !error.isNoAccess()) return error.left()
            state.update { it.copy(pendingSharedTrash = it.pendingSharedTrash - fileId) }
        }
        return Unit.right()
    }

    /**
     * Applies on this device what other devices did: teams left or deleted are removed, teams no longer shared
     * become private
     * @return the teams removed from this device
     */
    suspend fun applyRemovals(index: SharedTeamsIndex): Set<Uuid> {
        val removed = mutableSetOf<Uuid>()
        val sharedEntries = state.current().sharedEntries
        for ((teamId, entry) in index.teams) {
            val removal = entry.removal ?: continue
            if (teamId !in sharedEntries) continue
            if (removal == SharedTeamRemoval.LEFT || removal == SharedTeamRemoval.DELETED) {
                local.delete(teamId)
                removed.add(teamId)
            }
            state.update { it.copy(sharedEntries = it.sharedEntries - teamId) }
        }
        return removed
    }

    suspend fun syncTeam(teamId: Uuid, indexEntry: SharedIndexEntry): Either<CloudError, TeamSyncOutcome> {
        val fileId = indexEntry.fileId
        val file = storage.getSharedFile(fileId).getOrElse { error ->
            return if (error.isNoAccess()) accessLost(teamId, fileId).right() else error.left()
        }
        if (file.trashed) return accessLost(teamId, fileId).right()

        val synced = state.current().sharedEntries[teamId]?.synced?.takeIf { it.remoteFileId == fileId }
        state.update { it.copy(sharedEntries = it.sharedEntries + (teamId to SharedEntry(fileId, file.role, synced))) }

        val localTeam = local.get(teamId) ?: return download(teamId, file, synced, DownloadMode.MERGE_IF_CHANGED)
        val localDate = localTeam.lastUpdatedAt
        val remoteDate = file.updatedAt()
        if (localDate.sameMillisAs(remoteDate)) return markSynced(localTeam, file, synced)
        // never synced from this device: no way to tell which side changed
        if (synced == null) return download(teamId, file, null, DownloadMode.ALWAYS_MERGE)

        val localChanged = !localDate.sameMillisAs(synced.syncedUpdatedAt)
        val remoteChanged = !remoteDate.sameMillisAs(synced.syncedUpdatedAt)
        return when {
            // a viewer can't change the team: the shared version wins
            localChanged && !file.canEdit -> download(teamId, file, synced, DownloadMode.REPLACE)
            localChanged && remoteChanged -> download(teamId, file, synced, DownloadMode.ALWAYS_MERGE)
            localChanged -> upload(localTeam, file, synced.fingerprint).map { TeamSyncOutcome(changedLocally = it) }
            else -> download(teamId, file, synced, DownloadMode.MERGE_IF_CHANGED)
        }
    }

    private enum class DownloadMode { REPLACE, MERGE_IF_CHANGED, ALWAYS_MERGE }

    private suspend fun download(teamId: Uuid, file: SharedFile, synced: SyncedEntry?, mode: DownloadMode): Either<CloudError, TeamSyncOutcome> {
        val remote = downloadTeam(file.id, teamId).getOrElse { return it.left() }
        val merge = local.applyRemote(remote, synced?.fingerprint, clock.now()) { localTeam ->
            when (mode) {
                DownloadMode.REPLACE -> false
                DownloadMode.ALWAYS_MERGE -> true
                // the app saved the team since the sync was planned
                DownloadMode.MERGE_IF_CHANGED -> synced == null || !localTeam.lastUpdatedAt.sameMillisAs(synced.syncedUpdatedAt)
            }
        }.getOrElse { return it.left() }

        if (merge == null || !file.canEdit) {
            setSynced(remote, file)
            return TeamSyncOutcome(changedLocally = true).right()
        }
        // the others get the merged version too
        upload(merge.team, file, TeamFingerprint.of(remote)).getOrElse { return it.left() }
        return TeamSyncOutcome(changedLocally = true, merge = TeamMerge(teamId, merge.team.name, merge.summary)).right()
    }

    /**
     * Uploads the team, then checks that nobody wrote the file between the version it comes from and this upload.
     * If someone did, their version is merged into this one, which is uploaded again: nothing written is lost.
     * @param base the version the team comes from
     * @return whether the local team changed (merged with a concurrent version)
     */
    private suspend fun upload(entity: TeamlyticsEntity, file: SharedFile, base: TeamFingerprint?): Either<CloudError, Boolean> {
        var current = entity
        var currentBase = base
        var baseRevision = file.headRevisionId
        var changedLocally = false
        repeat(MAX_UPLOAD_ROUNDS) {
            val uploaded = storage.uploadSharedFile(fileName(current), toBytes(current), TeamSyncService.remoteProperties(current), file.id)
                .getOrElse { return it.left() }
            val concurrent = concurrentRevisions(file.id, baseRevision, uploaded.headRevisionId)
            if (concurrent.isEmpty()) {
                setSynced(current, uploaded)
                return changedLocally.right()
            }
            for (revisionId in concurrent) {
                val bytes = storage.downloadRevision(file.id, revisionId).getOrElse { return it.left() }
                val theirs = decodeTeam(bytes, current.id).getOrElse { continue }
                current = TeamMerger.merge(currentBase, current, theirs, localIsNewer = true, clock.now()).team
            }
            local.save(current).getOrElse { return it.left() }
            changedLocally = true
            currentBase = TeamFingerprint.of(current)
            baseRevision = uploaded.headRevisionId
        }
        return CloudError.Unknown("The shared team keeps changing, it will be synced later").left()
    }

    /**
     * @return the revisions written by someone else between [baseRevisionId] and [ourRevisionId]
     */
    private suspend fun concurrentRevisions(fileId: String, baseRevisionId: String?, ourRevisionId: String?): List<String> {
        if (baseRevisionId == null || ourRevisionId == null) return emptyList()
        // can't be listed: assumed fine, the next sync merges anyway what it finds
        val revisions = storage.listRevisions(fileId).getOrElse { return emptyList() }
        val baseIndex = revisions.indexOfFirst { it.id == baseRevisionId }
        val ourIndex = revisions.indexOfLast { it.id == ourRevisionId }
        if (baseIndex < 0 || ourIndex <= baseIndex) return emptyList()
        return revisions.subList(baseIndex + 1, ourIndex).map { it.id }
    }

    private suspend fun markSynced(localTeam: TeamlyticsEntity, file: SharedFile, synced: SyncedEntry?): Either<CloudError, TeamSyncOutcome> {
        if (synced?.fingerprint == null || !synced.syncedUpdatedAt.sameMillisAs(localTeam.lastUpdatedAt)) setSynced(localTeam, file)
        return TeamSyncOutcome().right()
    }

    /**
     * The file was deleted, or the user's access to it removed: the team stays on the device, as a private team
     */
    private suspend fun accessLost(teamId: Uuid, fileId: String): TeamSyncOutcome {
        removeFromShared(teamId, fileId, SharedTeamRemoval.UNSHARED)
        val name = local.name(teamId) ?: return TeamSyncOutcome()
        return TeamSyncOutcome(changedLocally = true, notice = "$name is no longer shared with you: it's kept as a private team")
    }

    private suspend fun setSynced(entity: TeamlyticsEntity, file: SharedFile) {
        val entry = SharedEntry(file.id, file.role, SyncedEntry(file.id, entity.lastUpdatedAt, TeamFingerprint.of(entity)))
        state.update { it.copy(sharedEntries = it.sharedEntries + (entity.id to entry)) }
    }

    private suspend fun removeFromShared(teamId: Uuid, fileId: String, removal: SharedTeamRemoval) {
        val entry = SharedIndexEntry(fileId, clock.now(), removal)
        state.update { it.copy(sharedEntries = it.sharedEntries - teamId, sharedIndex = it.sharedIndex.with(teamId, entry)) }
    }

    /**
     * Moves a private team to a file of the user's cloud storage that can be shared
     * @param privateFileId the private copy, deleted once the team is shared
     * @return the shared file's id
     */
    suspend fun makeShared(teamId: Uuid, privateFileId: String?): Either<CloudError, String> {
        state.current().sharedEntries[teamId]?.let { return it.fileId.right() }
        val entity = local.get(teamId) ?: return CloudError.NotFound("No team $teamId on this device").left()
        val folderId = storage.ensureFolder(FOLDER_NAME).getOrElse { return it.left() }
        val file = storage.uploadSharedFile(fileName(entity), toBytes(entity), TeamSyncService.remoteProperties(entity), null, folderId)
            .getOrElse { return it.left() }
        // only the owner invites
        storage.setEditorsCanShare(file.id, false).getOrElse { return it.left() }
        setSynced(entity, file)
        state.update {
            it.copy(
                sharedIndex = it.sharedIndex.with(teamId, SharedIndexEntry(file.id, clock.now())),
                entries = it.entries - teamId,
                tombstones = it.tombstones - teamId,
            )
        }
        // not a tombstone: the other devices learn from the index that the team moved
        privateFileId?.let { storage.delete(it) }
        return file.id.right()
    }

    suspend fun share(teamId: Uuid, email: String, role: ShareRole): Either<CloudError, Unit> {
        val fileId = state.current().sharedEntries[teamId]?.fileId ?: return CloudError.NotFound("Team $teamId isn't shared").left()
        return storage.share(fileId, email, role, INVITATION_MESSAGE)
    }

    suspend fun members(teamId: Uuid): Either<CloudError, List<Member>> {
        val fileId = sharedFileId(teamId) ?: return emptyList<Member>().right()
        return storage.listMembers(fileId)
    }

    suspend fun setMemberRole(teamId: Uuid, permissionId: String, role: ShareRole): Either<CloudError, Unit> {
        val fileId = sharedFileId(teamId) ?: return Unit.right()
        return storage.setMemberRole(fileId, permissionId, role)
    }

    suspend fun removeMember(teamId: Uuid, permissionId: String): Either<CloudError, Unit> {
        val fileId = sharedFileId(teamId) ?: return Unit.right()
        return storage.removeMember(fileId, permissionId)
    }

    /**
     * The owner stops sharing: the members lose the access (and keep a private copy), the team becomes private again
     */
    suspend fun stopSharing(teamId: Uuid) = removeAndTrash(teamId, SharedTeamRemoval.UNSHARED)

    /**
     * The owner deletes the team, for everyone. The local team is already deleted
     */
    suspend fun deleteForEveryone(teamId: Uuid) = removeAndTrash(teamId, SharedTeamRemoval.DELETED)

    /**
     * The file is trashed at the next sync, so that it works offline too
     */
    private suspend fun removeAndTrash(teamId: Uuid, removal: SharedTeamRemoval) {
        val fileId = sharedFileId(teamId) ?: return
        removeFromShared(teamId, fileId, removal)
        state.update { it.copy(pendingSharedTrash = it.pendingSharedTrash + fileId) }
    }

    /**
     * A member leaves the team: removed from their devices. The local team is already deleted
     */
    suspend fun leave(teamId: Uuid) {
        val fileId = sharedFileId(teamId) ?: return
        removeFromShared(teamId, fileId, SharedTeamRemoval.LEFT)
    }

    /**
     * Joins teams shared with the user, picked by them: their files can now be reached
     * @return the names of the teams joined
     */
    suspend fun join(fileIds: List<String>): Either<CloudError, List<String>> {
        val joined = mutableListOf<String>()
        for (fileId in fileIds) {
            val file = storage.getSharedFile(fileId).getOrElse { return it.left() }
            val teamId = file.properties[TeamSyncService.PROPERTY_TEAM_ID]?.let { Uuid.parseOrNull(it) }
                ?: return CloudError.Unknown("${file.name} isn't an Alakastats team").left()
            val remote = downloadTeam(fileId, teamId).getOrElse { return it.left() }
            // the team may already be here, e.g. kept as a private copy after a lost access: both are merged
            local.applyRemote(remote, base = null, clock.now()) { true }.getOrElse { return it.left() }
            state.update {
                it.copy(
                    sharedEntries = it.sharedEntries + (teamId to SharedEntry(fileId, file.role, SyncedEntry(fileId, remote.lastUpdatedAt, TeamFingerprint.of(remote)))),
                    sharedIndex = it.sharedIndex.with(teamId, SharedIndexEntry(fileId, clock.now())),
                    entries = it.entries - teamId,
                    tombstones = it.tombstones - teamId,
                )
            }
            joined.add(remote.name)
        }
        return joined.right()
    }

    private suspend fun sharedFileId(teamId: Uuid) = state.current().sharedEntries[teamId]?.fileId

    private suspend fun downloadTeam(fileId: String, teamId: Uuid): Either<CloudError, TeamlyticsEntity> {
        val bytes = storage.downloadSharedFile(fileId).getOrElse { return it.left() }
        return decodeTeam(bytes, teamId)
    }

    private fun decodeTeam(bytes: ByteArray, teamId: Uuid): Either<CloudError, TeamlyticsEntity> {
        val entity = try {
            json.decodeFromString<TeamlyticsEntity>(bytes.decodeToString())
        } catch (e: Exception) {
            return CloudError.Unknown("The shared copy can't be read (${e.message})", e).left()
        }
        if (entity.id != teamId) return CloudError.Unknown("The shared copy belongs to another team").left()
        return entity.right()
    }

    private fun toBytes(entity: TeamlyticsEntity) = json.encodeToString(entity).encodeToByteArray()

    private fun SharedFile.updatedAt() = properties[TeamSyncService.PROPERTY_UPDATED_AT]?.toLongOrNull()?.let { Instant.fromEpochMilliseconds(it) }

    private fun CloudError.isNoAccess() = this is CloudError.NotFound || this is CloudError.FileAccessDenied

    companion object {
        const val FOLDER_NAME = "Alakastats"
        const val INDEX_FILE_NAME = "shared-teams.json"
        const val PROPERTY_KIND = "kind"
        const val INDEX_KIND = "sharedTeams"
        val INDEX_PROPERTIES = mapOf(PROPERTY_KIND to INDEX_KIND, TeamSyncService.PROPERTY_SCHEMA to TeamSyncService.SCHEMA_VERSION.toString())
        private const val MAX_UPLOAD_ROUNDS = 3
        const val INVITATION_MESSAGE = "I shared an Alakastats team with you. To join it, open https://tambapps.github.io/alakastats/, " +
                "connect your Google Drive in the Sync screen, then use \"Join a shared team\" on the home screen. " +
                "It will then sync on all your devices."

        // visible in the owner's Drive
        fun fileName(entity: TeamlyticsEntity) = "${entity.name} (Alakastats).json"
    }
}
