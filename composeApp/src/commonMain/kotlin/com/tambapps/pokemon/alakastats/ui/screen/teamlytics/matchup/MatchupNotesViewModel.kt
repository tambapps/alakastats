package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.matchup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.alakastats.domain.usecase.ManageMatchupNotesListUseCase
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.TeamlyticsTabViewModel
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService

class MatchupNotesViewModel(
    override val useCase: ManageMatchupNotesListUseCase,
    val pokemonImageService: PokemonImageService,
): TeamlyticsTabViewModel() {

    override var isTabLoading by mutableStateOf(false)
        private set

    val hasMatchups get() = false
}