package com.tambapps.pokemon.alakastats.domain.usecase

import arrow.core.Either
import com.tambapps.pokemon.alakastats.domain.error.GetTeamlyticsError
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import kotlin.uuid.Uuid

interface ManageTeamlyticsListUseCase {
    suspend fun list(): List<TeamlyticsPreview>
    suspend fun get(id: Uuid): Either<GetTeamlyticsError, Teamlytics>
    suspend fun delete(id: Uuid)
}