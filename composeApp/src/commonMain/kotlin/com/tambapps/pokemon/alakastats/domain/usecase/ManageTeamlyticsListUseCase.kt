package com.tambapps.pokemon.alakastats.domain.usecase

import arrow.core.Either
import com.tambapps.pokemon.alakastats.domain.error.GetTeamlyticsError
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import kotlin.uuid.Uuid

class ManageTeamlyticsListUseCase(
    private val repository: TeamlyticsRepository,
) {

    suspend fun list(): List<TeamlyticsPreview> = repository.listPreviews()

    suspend fun get(id: Uuid): Either<GetTeamlyticsError, Teamlytics> = repository.get(id)

    suspend fun delete(id: Uuid) = repository.delete(id)
}
