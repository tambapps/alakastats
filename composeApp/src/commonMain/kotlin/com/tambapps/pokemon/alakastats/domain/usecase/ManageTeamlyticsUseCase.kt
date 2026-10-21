package com.tambapps.pokemon.alakastats.domain.usecase

import arrow.core.Either
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.alakastats.infrastructure.service.TeamlyticsSerializer
import kotlin.uuid.Uuid

class ManageTeamlyticsUseCase(
    private val repository: TeamlyticsRepository,
    private val teamlyticsSerializer: TeamlyticsSerializer
) {

    suspend fun get(id: Uuid): Either<DomainError, Teamlytics> = repository.get(id)

    /**
     * @param base the version the team was changed from, to merge with what was saved since
     */
    suspend fun save(team: Teamlytics, base: Teamlytics) = repository.save(team, base)

    fun export(team: Teamlytics) = teamlyticsSerializer.export(team)
}
