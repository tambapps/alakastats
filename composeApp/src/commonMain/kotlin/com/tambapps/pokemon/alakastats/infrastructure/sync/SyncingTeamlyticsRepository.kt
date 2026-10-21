package com.tambapps.pokemon.alakastats.infrastructure.sync

import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import kotlin.uuid.Uuid

/**
 * Tells the [TeamSyncService] about every local change. Does nothing more when the cloud isn't connected
 */
class SyncingTeamlyticsRepository(
    private val delegate: TeamlyticsRepository,
    private val syncService: TeamSyncService,
) : TeamlyticsRepository by delegate {

    override suspend fun save(teamlytics: Teamlytics) = delegate.save(teamlytics)
        .onRight { syncService.requestSync() }

    override suspend fun delete(teamlytics: Teamlytics) {
        delegate.delete(teamlytics)
        syncService.onLocalDelete(teamlytics.id)
    }

    override suspend fun delete(id: Uuid) {
        delegate.delete(id)
        syncService.onLocalDelete(id)
    }
}
