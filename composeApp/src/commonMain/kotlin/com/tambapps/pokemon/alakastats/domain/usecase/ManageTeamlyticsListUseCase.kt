package com.tambapps.pokemon.alakastats.domain.usecase

import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import kotlin.uuid.Uuid

interface ManageTeamlyticsListUseCase {
    suspend fun list(): List<TeamlyticsPreview>
    suspend fun get(id: Uuid): Teamlytics?
    suspend fun delete(id: Uuid)
}