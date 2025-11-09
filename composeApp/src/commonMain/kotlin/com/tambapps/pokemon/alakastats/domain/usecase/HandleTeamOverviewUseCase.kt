package com.tambapps.pokemon.alakastats.domain.usecase

import arrow.core.Either
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsNotes

interface HandleTeamOverviewUseCase: FilterableReplaysUseCase {
    suspend fun setNotes(team: Teamlytics, notes: TeamlyticsNotes?): Either<DomainError, Unit>

    fun export(team: Teamlytics): ByteArray
}
