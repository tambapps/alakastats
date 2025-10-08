package com.tambapps.pokemon.alakastats.infrastructure.usecase

import arrow.core.Either
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.alakastats.domain.usecase.TeamlyticsUseCase
import com.tambapps.pokemon.alakastats.infrastructure.service.TeamlyticsSerializer
import kotlin.uuid.Uuid

class TeamlyticsUseCaseImpl(
    private val repository: TeamlyticsRepository,
    private val teamlyticsSerializer: TeamlyticsSerializer
) : TeamlyticsUseCase {

    override suspend fun get(id: Uuid): Either<DomainError, Teamlytics> = repository.get(id)

    override suspend fun save(team: Teamlytics) = repository.save(team)

    override fun export(team: Teamlytics) = teamlyticsSerializer.export(team)
}