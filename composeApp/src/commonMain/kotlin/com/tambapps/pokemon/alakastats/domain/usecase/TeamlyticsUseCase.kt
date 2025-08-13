package com.tambapps.pokemon.alakastats.domain.usecase

import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import kotlin.uuid.Uuid

class TeamlyticsUseCase(
    private val repository: TeamlyticsRepository
) {

    suspend fun get(id: Uuid) = repository.get(id)
}