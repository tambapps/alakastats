package com.tambapps.pokemon.alakastats.domain.usecase

import com.tambapps.pokemon.alakastats.ui.model.ReplayFilters

interface ManageReplayFiltersUseCase: ConsultTeamlyticsUseCase {
    val filters: ReplayFilters

    fun applyFilters(filters: ReplayFilters)

    fun openFilters()

    fun closeFilters()
}