package com.tambapps.pokemon.alakastats.domain.usecase

import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics

interface FilterableReplaysUseCase {

    // TODO use below fields
    val allReplays: List<ReplayAnalytics>

    val filteredReplays: List<ReplayAnalytics>

    val hasFiltered: Boolean
}