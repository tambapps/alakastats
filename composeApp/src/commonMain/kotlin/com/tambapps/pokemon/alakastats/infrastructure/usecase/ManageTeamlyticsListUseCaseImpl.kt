package com.tambapps.pokemon.alakastats.infrastructure.usecase

import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamlyticsListUseCase
import kotlin.uuid.Uuid

class ManageTeamlyticsListUseCaseImpl(
    private val repository: TeamlyticsRepository
) : ManageTeamlyticsListUseCase {

    override suspend fun list(): List<TeamlyticsPreview> = repository.listPreviews()

    override suspend fun get(id: Uuid): Teamlytics? = repository.get(id)

    override suspend fun delete(id: Uuid) = repository.delete(id)
}