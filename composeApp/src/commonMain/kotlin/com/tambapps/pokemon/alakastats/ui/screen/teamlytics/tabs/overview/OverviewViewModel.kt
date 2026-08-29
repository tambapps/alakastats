package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.overview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import arrow.core.mapValuesNotNull
import arrow.core.raise.either
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.alakastats.domain.model.PokemonData
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsData
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsNotes
import com.tambapps.pokemon.alakastats.domain.repository.PokemonBaseStatsRepository
import com.tambapps.pokemon.alakastats.domain.repository.PokemonMovesRepository
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamOverviewUseCase
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.downloadToFile
import com.tambapps.pokemon.alakastats.ui.SnackBar
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.TeamlyticsTabViewModel
import com.tambapps.pokemon.util.MegaUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverviewViewModel(
    override val useCase: ManageTeamOverviewUseCase,
    private val pokemonMovesRepository: PokemonMovesRepository,
    private val pokemonBaseStatsRepository: PokemonBaseStatsRepository
): TeamlyticsTabViewModel() {
    // important. In this tab we don't want to consider filters
    val team get() = useCase.originalTeam
    var isEditingNotes by mutableStateOf(false)
    var teamNotes by mutableStateOf("")
    val pokemonNotes = mutableStateMapOf<Pokemon, String>()
    val leveledPokemons = when(val formatPokemonLevel = team.format.pokemonLevel) {
        null -> team.pokePaste.pokemons
        else -> team.pokePaste.pokemons.map { it.copy(level = formatPokemonLevel) }
    }
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
        if (!team.shouldLoadPokemonData()) {
            return
        }
        isTabLoading = true
        scope.launch {
            val pokemons = team.pokePaste.pokemons
            val moveNames = pokemons.asSequence()
                .flatMap { it.moves }
                .map { it.normalized }
                .distinctBy { it.value }
                .toList()
            // map pokemon -> pokemon forms
            val pokemonForms = pokemons.associateWith { pokemon ->
                if (pokemon.name.isMega) listOfNotNull(pokemon.name.baseNormalized, pokemon.name.normalized)
                else listOfNotNull(pokemon.name.normalized, MegaUtils.getMegaPokemon(pokemon.item))
            }
            val either = either {
                val moves = pokemonMovesRepository.getMoves(moveNames).bind()
                val baseStats = pokemonBaseStatsRepository.getBaseStats(pokemonForms.values.flatten().distinct()).bind()
                pokemonForms.map { (pokemon, forms) ->
                    PokemonData(
                        name = pokemon.name,
                        moves = pokemon.moves.mapNotNull { moves[it.normalized] }.associateBy { it.name.normalized },
                        baseStatsPerForms = forms.associateWith { baseStats[it] }.mapValuesNotNull { (_, value) -> value },
                    )
                }
            }
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

private fun Teamlytics.shouldLoadPokemonData() = pokePaste.pokemons.any { !data.pokemonData.containsKey(it.name.normalized) }
        || data.pokemonData.values.any { it.shouldReload() }

