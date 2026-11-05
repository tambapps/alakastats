package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.repository.PokemonBaseStatsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// a pokemon's megas are named after it, e.g. charizard-mega-x
private val MEGA_SUFFIXES = listOf("-mega", "-mega-x", "-mega-y", "-mega-z")

class PokedexViewModel(
    private val pokemonBaseStatsRepository: PokemonBaseStatsRepository,
) : ViewModel() {

    var query by mutableStateOf("")
    // the form shown, null until a pokemon is searched
    var selectedForm by mutableStateOf<PokemonName?>(null)
        private set
    // the searched pokemon, then its megas
    var forms by mutableStateOf(emptyList<PokemonName>())
        private set
    var isLoading by mutableStateOf(false)
        private set
    private var statsByForm by mutableStateOf(emptyMap<PokemonName, PokeStats>())

    // null if they're still loading or couldn't be found
    val baseStats get() = selectedForm?.let { statsByForm[it.normalized] }

    private var loadJob: Job? = null

    fun selectPokemon(pokemon: PokemonName) {
        // the field is emptied, ready for the next search, the sheet telling which pokemon is shown
        query = ""
        selectedForm = pokemon
        forms = emptyList()
        statsByForm = emptyMap()
        isLoading = true
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val megaCandidates = MEGA_SUFFIXES.map { PokemonName(pokemon.normalized.value + it) }
            pokemonBaseStatsRepository.getBaseStats(listOf(pokemon) + megaCandidates).onRight { stats ->
                // the repository gives zeros for a pokemon it doesn't know
                val knownStats = stats.filterValues { it.hp > 0 }
                statsByForm = knownStats
                forms = listOf(pokemon) + megaCandidates.filter { it.normalized in knownStats }
            }
            isLoading = false
        }
    }

    fun selectForm(form: PokemonName) {
        selectedForm = form
    }
}
