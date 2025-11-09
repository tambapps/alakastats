package com.tambapps.pokemon.alakastats.domain.usecase

import arrow.core.Either
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics

interface ManageTeamReplaysUseCase: ManageReplayFiltersUseCase, ConsultTeamlyticsUseCase {

    suspend fun addReplays(replays: List<ReplayAnalytics>): Either<DomainError, Unit>

    suspend fun removeReplay(replay: ReplayAnalytics): Either<DomainError, Unit>

    suspend fun replaceReplay(original: ReplayAnalytics, replay: ReplayAnalytics): Either<DomainError, Unit>

}