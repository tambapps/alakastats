package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.notes

import androidx.compose.runtime.MutableState
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService

class TeamNotesViewModel(
    val pokemonImageService: PokemonImageService,
    private val teamState: MutableState<Teamlytics?>,
    val team: Teamlytics,
)