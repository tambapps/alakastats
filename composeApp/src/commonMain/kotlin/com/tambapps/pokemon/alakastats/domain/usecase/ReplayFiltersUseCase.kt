package com.tambapps.pokemon.alakastats.domain.usecase

import com.tambapps.pokemon.alakastats.ui.model.ReplayFilters

interface ReplayFiltersUseCase {
    val filters: ReplayFilters

    fun openFilters()

}