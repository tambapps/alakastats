package com.tambapps.pokemon.alakastats.domain.usecase

import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository

class ListTeamlyticsUseCase(
    private val repository: TeamlyticsRepository
) {

    suspend fun listPreviews() = repository.listPreview()
}