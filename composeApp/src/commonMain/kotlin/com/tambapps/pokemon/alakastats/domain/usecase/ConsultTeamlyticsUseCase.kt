package com.tambapps.pokemon.alakastats.domain.usecase

import com.tambapps.pokemon.alakastats.domain.model.Teamlytics

interface ConsultTeamlyticsUseCase {

    val originalTeam: Teamlytics

    // the one with replays that may be filtered
    val team: Teamlytics

    val hasFilteredReplays: Boolean
    val isApplyingFiltersLoading: Boolean
}