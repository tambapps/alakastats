package com.tambapps.pokemon.alakastats.infrastructure.sync

import arrow.core.Either
import arrow.core.left
import com.tambapps.pokemon.alakastats.domain.error.GetTeamlyticsError
import com.tambapps.pokemon.alakastats.domain.error.ReadOnlyTeamError
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import kotlin.uuid.Uuid

/**
 * Tells the [TeamSyncService] about every local change, and refuses to change a team shared with the user as a
 * viewer. Does nothing more when the cloud isn't connected
 */
class SyncingTeamlyticsRepository(
    private val delegate: TeamlyticsRepository,
    private val syncService: TeamSyncService,
) : TeamlyticsRepository by delegate {

    override suspend fun save(teamlytics: Teamlytics, base: Teamlytics?): Either<GetTeamlyticsError, Teamlytics> {
        // the screens hide the edit actions: this is a safety net
        if (syncService.isReadOnly(teamlytics.id)) return ReadOnlyTeamError(teamlytics.id).left()
        return delegate.save(teamlytics, base).onRight { syncService.requestSync() }
    }

    override suspend fun delete(teamlytics: Teamlytics) {
        delegate.delete(teamlytics)
        syncService.onLocalDelete(teamlytics.id)
    }

    override suspend fun delete(id: Uuid) {
        delegate.delete(id)
        syncService.onLocalDelete(id)
    }
}
