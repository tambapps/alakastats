package com.tambapps.pokemon.alakastats.infrastructure.usecase

import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.alakastats.domain.usecase.TeamlyticsUseCase
import kotlin.uuid.Uuid

class TeamlyticsUseCaseImpl(
    private val repository: TeamlyticsRepository
) : TeamlyticsUseCase {

    override suspend fun get(id: Uuid): Teamlytics? = repository.get(id)

    override suspend fun save(team: Teamlytics) = repository.save(team)
}