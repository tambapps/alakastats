package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.overview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsData
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsNotes
import com.tambapps.pokemon.alakastats.domain.repository.PokemonDataRepository
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamOverviewUseCase
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.downloadToFile
import com.tambapps.pokemon.alakastats.ui.SnackBar
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.TeamlyticsTabViewModel
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverviewViewModel(
    override val useCase: ManageTeamOverviewUseCase,
    override val pokemonImageService: PokemonImageService,
    private val pokemonDataRepository: PokemonDataRepository
): TeamlyticsTabViewModel() {
    // important. In this tab we don't want to consider filters
    val team get() = useCase.originalTeam
    var isEditingNotes by mutableStateOf(false)
    var teamNotes by mutableStateOf("")
    val pokemonNotes = mutableStateMapOf<Pokemon, String>()

    override var isTabLoading by mutableStateOf(false)

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        initNotesState()
        loadPokemonData()
    }

    fun editNotes() {
        isEditingNotes = true
        initNotesState()
        loadPokemonData()
    }

    fun exportTeam(snackBar: SnackBar) {
        if (isLoading) {
            return
        }
        isTabLoading = true
        scope.launch {
            val success = downloadToFile(team.name, "json", useCase.export(team))

            withContext(Dispatchers.Main) {
                isTabLoading = false
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

    fun saveNotes(snackBar: SnackBar) {
        if (isLoading) {
            return
        }
        isTabLoading = true
        scope.launch {
            val either = useCase.setNotes(team, TeamlyticsNotes(teamNotes, pokemonNotes.mapKeys { (key, _) -> key.name }))
            withContext(Dispatchers.Main) {
                either.onLeft { error -> snackBar.show("Couldn't save notes: ${error.message}", SnackBar.Severity.ERROR) }
                isTabLoading = false
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
        if (isLoading) {
            return
        }
        isTabLoading = true
        scope.launch {
            useCase.setNotes(team, null)
            withContext(Dispatchers.Main) {
                isTabLoading = false
            }
        }
    }


    private fun loadPokemonData() {
        val data = team.data
        if (team.pokePaste.pokemons.all { data.pokemonData.containsKey(it.name.normalized) }) {
            return
        }
        isTabLoading = true
        scope.launch {
            val either = pokemonDataRepository.bulkGet(team.pokePaste.pokemons)
            withContext(Dispatchers.Main) {
                either.fold(
                    ifLeft = {

                    },
                    ifRight = { data ->
                        useCase.setData(team, TeamlyticsData(
                            pokemonData = data.associateBy { it.name.normalized }
                        ))
                    }
                )
                isTabLoading = false
            }
        }

    }
}

