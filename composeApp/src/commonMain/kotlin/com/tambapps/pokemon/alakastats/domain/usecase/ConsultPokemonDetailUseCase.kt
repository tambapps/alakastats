package com.tambapps.pokemon.alakastats.domain.usecase

import arrow.core.Either
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.alakastats.infrastructure.service.TeamlyticsSerializer
import kotlin.uuid.Uuid

class ConsultPokemonDetailUseCase(
    private val repository: TeamlyticsRepository,
) {

    suspend fun get(id: Uuid): Either<DomainError, Teamlytics> = repository.get(id)

}
