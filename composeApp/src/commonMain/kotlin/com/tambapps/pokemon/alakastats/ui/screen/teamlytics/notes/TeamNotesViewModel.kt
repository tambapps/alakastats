package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.notes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.usecase.HandleTeamNotesUseCase
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService

class TeamNotesViewModel(
    val pokemonImageService: PokemonImageService,
    private val useCase: HandleTeamNotesUseCase,
    val team: Teamlytics,
) {
    var isEditing by mutableStateOf(false)
        private set

    var teamNotes by mutableStateOf("")

    val pokemonNotes = mutableStateMapOf<PokemonName, String>()

    fun editMode(edit: Boolean) {
        this.isEditing = edit

        pokemonNotes.clear()
        for (pokemon in team.pokePaste.pokemons) {
            pokemonNotes[pokemon.name] = ""
        }
    }
}