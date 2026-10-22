package com.tambapps.pokemon.alakastats.infrastructure.sync

import com.tambapps.pokemon.alakastats.infrastructure.cloud.ShareRole
import io.github.xxfast.kstore.KStore
import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * What the device knows about the last synchronisations. Cleared on disconnection.
 * @param entries the private teams that were in sync with the cloud, with the version that was synced
 * @param tombstones the private teams deleted locally since the last sync, with the deletion date
 * @param sharedIndex the last known index of the shared teams, merged with the cloud's at each sync
 * @param sharedEntries the shared teams of this device, with the version that was synced
 * @param pendingSharedTrash the shared files to trash (team deleted or no longer shared), e.g. while offline
 * @param formerlyLinkedTeams once disconnected, the teams that were synced: they have a copy in the cloud that isn't
 * kept up to date anymore. Cleared at the next connection
 * @param formerlySharedRoles once disconnected, the user's role in the teams that were shared
 * @param accountEmail the e-mail of the connected account, only to show it. Stays on the device, cleared on
 * disconnection
 */
@Serializable
data class SyncState(
    val connected: Boolean = false,
    val lastSyncAt: Instant? = null,
    val entries: Map<Uuid, SyncedEntry> = emptyMap(),
    val tombstones: Map<Uuid, Instant> = emptyMap(),
    val sharedIndex: SharedTeamsIndex = SharedTeamsIndex(),
    val sharedEntries: Map<Uuid, SharedEntry> = emptyMap(),
    val pendingSharedTrash: Set<String> = emptySet(),
    val formerlyLinkedTeams: Set<Uuid> = emptySet(),
    val formerlySharedRoles: Map<Uuid, ShareRole> = emptyMap(),
    val accountEmail: String? = null,
)

/**
 * @param fingerprint the synced version, base of the merge when both sides changed it. Null in states saved before
 * merges existed
 */
@Serializable
data class SyncedEntry(
    val remoteFileId: String,
    val syncedUpdatedAt: Instant?,
    val fingerprint: TeamFingerprint? = null,
)

interface SyncStateStore {
    suspend fun load(): SyncState
    suspend fun save(state: SyncState)
}

class KStoreSyncStateStore(private val store: KStore<SyncState>) : SyncStateStore {
    override suspend fun load() = store.get() ?: SyncState()
    override suspend fun save(state: SyncState) = store.set(state)
}
