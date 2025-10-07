package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.overview

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService

class OverviewViewModel(
    val pokemonImageService: PokemonImageService,
    private val teamState: MutableState<Teamlytics?>,
    val team: Teamlytics,
) {
    var isEditingNotes by mutableStateOf(false)


    fun editNotes() {
        isEditingNotes = true
    }

    fun removeNotes() {
        // TODO
    }
}

