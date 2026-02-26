package com.tambapps.pokemon.alakastats.domain.usecase

import arrow.core.Either
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.domain.model.MatchupPlan

interface ManageMatchupPlansUseCase: ConsultTeamlyticsUseCase {

    suspend fun setMatchupPlans(matchupPlans: List<MatchupPlan>): Either<DomainError, Unit>

}