package com.tambapps.pokemon.alakastats.domain.usecase

import arrow.core.Either
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import kotlin.uuid.Uuid

interface TeamlyticsUseCase {
    suspend fun get(id: Uuid): Either<DomainError, Teamlytics>
    suspend fun save(team: Teamlytics): Teamlytics

    fun exportToJson(team: Teamlytics): String
}