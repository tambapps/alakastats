package com.tambapps.pokemon.alakastats.infrastructure.sync

import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * @param localOnly the team is kept on this device only: it's out of the sync
 */
data class LocalTeamVersion(val id: Uuid, val updatedAt: Instant?, val localOnly: Boolean = false)

data class RemoteTeamVersion(val id: Uuid, val fileId: String, val updatedAt: Instant?)

sealed interface SyncAction {
    val teamId: Uuid

    data class Upload(override val teamId: Uuid, val existingFileId: String?) : SyncAction
    data class Download(override val teamId: Uuid, val fileId: String) : SyncAction
    data class DeleteLocal(override val teamId: Uuid) : SyncAction
    data class DeleteRemote(override val teamId: Uuid, val fileId: String) : SyncAction
    /** Both sides changed since the last sync */
    data class Merge(override val teamId: Uuid, val fileId: String) : SyncAction
    /** Both sides already have the same version */
    data class MarkSynced(override val teamId: Uuid, val fileId: String, val updatedAt: Instant?) : SyncAction
}

/**
 * Tells what to do with each team, based on the teams' lastUpdatedAt and on the version synced last: the side that
 * changed since is taken, and when both did, they are merged. The local files stay the source of truth offline:
 * nothing here is applied.
 */
object SyncPlanner {

    fun plan(local: List<LocalTeamVersion>, remote: List<RemoteTeamVersion>, state: SyncState): List<SyncAction> {
        val actions = mutableListOf<SyncAction>()
        val localById = local.associateBy { it.id }
        // the same team could have been uploaded twice (e.g. a response lost after the creation): keep the newest
        val remoteById = remote.groupBy { it.id }.mapValues { (_, files) ->
            val sorted = files.sortedByDescending { it.updatedAt.orPast() }
            sorted.drop(1).forEach { actions.add(SyncAction.DeleteRemote(it.id, it.fileId)) }
            sorted.first()
        }

        for (id in localById.keys + remoteById.keys) {
            val localTeam = localById[id]
            val remoteTeam = remoteById[id]
            val action = when {
                localTeam != null && remoteTeam != null -> resolveBoth(localTeam, remoteTeam, state.entries[id])
                localTeam != null -> resolveLocalOnly(localTeam, state.entries[id])
                remoteTeam != null -> resolveRemoteOnly(remoteTeam, state.tombstones[id])
                else -> null
            }
            action?.let(actions::add)
        }
        return actions
    }

    private fun resolveBoth(local: LocalTeamVersion, remote: RemoteTeamVersion, entry: SyncedEntry?): SyncAction {
        val localDate = local.updatedAt.orPast()
        val remoteDate = remote.updatedAt.orPast()
        if (localDate == remoteDate) return SyncAction.MarkSynced(local.id, remote.fileId, local.updatedAt)
        // never synced from this device: no way to tell which side changed
        if (entry == null) return SyncAction.Merge(local.id, remote.fileId)

        val syncedDate = entry.syncedUpdatedAt.orPast()
        val localChanged = localDate != syncedDate
        val remoteChanged = remoteDate != syncedDate
        return when {
            localChanged && remoteChanged -> SyncAction.Merge(local.id, remote.fileId)
            localChanged -> SyncAction.Upload(local.id, remote.fileId)
            else -> SyncAction.Download(local.id, remote.fileId)
        }
    }

    private fun resolveLocalOnly(local: LocalTeamVersion, entry: SyncedEntry?): SyncAction = when {
        // never synced: new team
        entry == null -> SyncAction.Upload(local.id, existingFileId = null)
        // synced, then deleted on another device. Unless edited here since, in which case the edit wins
        local.updatedAt.orPast() > entry.syncedUpdatedAt.orPast() -> SyncAction.Upload(local.id, existingFileId = null)
        else -> SyncAction.DeleteLocal(local.id)
    }

    private fun resolveRemoteOnly(remote: RemoteTeamVersion, deletedAt: Instant?): SyncAction = when {
        deletedAt == null -> SyncAction.Download(remote.id, remote.fileId)
        // edited on another device after the local deletion: the edit wins
        remote.updatedAt.orPast() > deletedAt -> SyncAction.Download(remote.id, remote.fileId)
        else -> SyncAction.DeleteRemote(remote.id, remote.fileId)
    }

    // compared at the millisecond: the cloud only stores milliseconds
    private fun Instant?.orPast() = Instant.fromEpochMilliseconds((this ?: Instant.DISTANT_PAST).toEpochMilliseconds())
}
