package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.matchup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.alakastats.domain.usecase.ManageReplayFiltersUseCase
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.TeamlyticsFiltersTabViewModel
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService

class MatchupsViewModel(
    override val useCase: ManageReplayFiltersUseCase,
    override val pokemonImageService: PokemonImageService,
): TeamlyticsFiltersTabViewModel() {

    override var isTabLoading by mutableStateOf(false)
        private set
}