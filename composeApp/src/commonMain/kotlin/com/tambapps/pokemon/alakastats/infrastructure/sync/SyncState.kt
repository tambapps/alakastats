package com.tambapps.pokemon.alakastats.infrastructure.sync

import io.github.xxfast.kstore.KStore
import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * What the device knows about the last synchronisations. Cleared on disconnection.
 * @param entries the teams that were in sync with the cloud, with the version that was synced
 * @param tombstones the teams deleted locally since the last sync, with the deletion date
 */
@Serializable
data class SyncState(
    val connected: Boolean = false,
    val lastSyncAt: Instant? = null,
    val entries: Map<Uuid, SyncedEntry> = emptyMap(),
    val tombstones: Map<Uuid, Instant> = emptyMap(),
)

@Serializable
data class SyncedEntry(
    val remoteFileId: String,
    val syncedUpdatedAt: Instant?,
)

interface SyncStateStore {
    suspend fun load(): SyncState
    suspend fun save(state: SyncState)
}

class KStoreSyncStateStore(private val store: KStore<SyncState>) : SyncStateStore {
    override suspend fun load() = store.get() ?: SyncState()
    override suspend fun save(state: SyncState) = store.set(state)
}
