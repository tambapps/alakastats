package com.tambapps.pokemon.alakastats.domain.usecase

import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import kotlin.uuid.Uuid

class ManageTeamlyticsListUseCase(
    private val repository: TeamlyticsRepository
) {

    suspend fun list() = repository.listPreviews()

    suspend fun get(id: Uuid) = repository.get(id)

    suspend fun delete(id: Uuid) = repository.delete(id)
}