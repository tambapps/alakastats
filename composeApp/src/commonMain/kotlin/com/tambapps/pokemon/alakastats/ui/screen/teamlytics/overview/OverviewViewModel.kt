package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.overview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsNotes
import com.tambapps.pokemon.alakastats.domain.usecase.HandleTeamOverviewUseCase
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.downloadToFile
import com.tambapps.pokemon.alakastats.ui.SnackBar
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverviewViewModel(
    private val useCase: HandleTeamOverviewUseCase,
    val pokemonImageService: PokemonImageService,
    val team: Teamlytics,
) {
    var isEditingNotes by mutableStateOf(false)
    var teamNotes by mutableStateOf("")
    val pokemonNotes = mutableStateMapOf<Pokemon, String>()
    var foo by mutableStateOf("")

    var isLoading by mutableStateOf(false)

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        initNotesState()
    }

    fun editNotes() {
        isEditingNotes = true
        initNotesState()
    }

    fun exportTeam(snackBar: SnackBar) {
        isLoading = true
        scope.launch {
            val success = downloadToFile(team.name, "json", useCase.export(team))

            withContext(Dispatchers.Main) {
                isLoading = false
                if (success) {
                    snackBar.show("Successfully exported team", SnackBar.Severity.SUCCESS)
                }
            }
        }
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

