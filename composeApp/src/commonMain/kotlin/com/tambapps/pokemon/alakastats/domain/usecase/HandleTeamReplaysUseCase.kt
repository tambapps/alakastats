package com.tambapps.pokemon.alakastats.domain.usecase

import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics

interface HandleTeamReplaysUseCase {

    fun parseReplay(url: String): ReplayAnalytics

    fun addReplays(replays: List<ReplayAnalytics>)
}