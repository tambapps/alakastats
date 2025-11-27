package com.tambapps.pokemon.alakastats.domain.usecase

import arrow.core.Either
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.domain.model.MatchupNotes

interface ManageMatchupNotesUseCase: ConsultTeamlyticsUseCase {


    suspend fun setMatchupNotes(matchupNotes: List<MatchupNotes>): Either<DomainError, Unit>

}