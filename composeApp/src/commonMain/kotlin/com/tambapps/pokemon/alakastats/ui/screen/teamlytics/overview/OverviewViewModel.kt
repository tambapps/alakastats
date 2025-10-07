package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.overview

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsNotes
import com.tambapps.pokemon.alakastats.domain.usecase.HandleTeamNotesUseCase
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverviewViewModel(
    private val useCase: HandleTeamNotesUseCase,
    val pokemonImageService: PokemonImageService,
    val team: Teamlytics,
) {
    var isEditingNotes by mutableStateOf(false)
    var teamNotes by mutableStateOf("")
    val pokemonNotes = mutableStateMapOf<Pokemon, String>()

    // TODO use it on mobile UI
    var isLoading by mutableStateOf(false)

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        initNotesState()
    }

    fun editNotes() {
        isEditingNotes = true
        initNotesState()
    }

    private fun initNotesState() {
        pokemonNotes.clear()
        val notes = team.notes
        if (notes != null) {
            teamNotes = notes.teamNotes
            for ((pokemonName, pNotes) in notes.pokemonNotes) {
                val pokemon = team.pokePaste.pokemons.find { it.name == pokemonName } ?: continue
                pokemonNotes[pokemon] = pNotes
            }
        } else {
            teamNotes = ""
        }
        for (pokemon in team.pokePaste.pokemons) {
            if (!pokemonNotes.containsKey(pokemon)) {
                pokemonNotes[pokemon] = ""
            }
        }
    }

    fun saveNotes() {
        isLoading = true
        scope.launch {
            useCase.setNotes(team, TeamlyticsNotes(teamNotes, pokemonNotes.mapKeys { (key, _) -> key.name }))
            withContext(Dispatchers.Main) {
                isLoading = false
                onStopEditingNotes()
            }
        }
    }

    fun cancelEditingNotes() {
        onStopEditingNotes()
        initNotesState()
    }

    private fun onStopEditingNotes() {
        isEditingNotes = false
    }

    fun removeNotes() {
        isLoading = true
        scope.launch {
            useCase.setNotes(team, null)
            withContext(Dispatchers.Main) {
                isLoading = false
            }
        }
    }
}

