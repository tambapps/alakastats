package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.overview

import androidx.compose.runtime.MutableState
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.ui.service.IPokemonImageService

class OverviewViewModel(
    val pokemonImageService: IPokemonImageService,
    private val teamState: MutableState<Teamlytics?>,
    val team: Teamlytics,
)