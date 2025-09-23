package com.tambapps.pokemon.alakastats.domain.usecase

import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics

interface HandleTeamReplaysUseCase {

    suspend fun parseReplay(url: String): ReplayAnalytics

    suspend fun addReplays(replays: List<ReplayAnalytics>)

    suspend fun removeReplay(replay: ReplayAnalytics)

    suspend fun replaceReplay(original: ReplayAnalytics, replay: ReplayAnalytics)

}