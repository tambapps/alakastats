package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.repository.PokemonBaseStatsRepository

// a pokemon's megas are named after it, e.g. charizard-mega-x. The longest first, for baseFormOf
private val MEGA_SUFFIXES = listOf("-mega-x", "-mega-y", "-mega-z", "-mega")

/**
 * The pokemon a form is the form of, e.g. metagross for metagross-mega
 */
internal fun baseFormOf(form: PokemonName): PokemonName {
    val suffix = MEGA_SUFFIXES.find { form.value.endsWith(it) } ?: return form
    return PokemonName(form.value.removeSuffix(suffix))
}

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
    // the form to show instead of the one asked, which doesn't exist (e.g. in an URL)
    var formToShowInstead by mutableStateOf<PokemonName?>(null)
        private set
    private var statsByForm by mutableStateOf(emptyMap<PokemonName, PokeStats>())

    // null if they're still loading or couldn't be found
    val baseStats get() = selectedForm?.let { statsByForm[it.normalized] }

    // the pokemon whose forms are loaded
    private var loadedPokemon: PokemonName? = null

    /**
     * Shows a form, loading what its pokemon needs unless it already is
     *
     * @param form the form, null for none
     */
    suspend fun show(form: PokemonName?) {
        formToShowInstead = null
        selectedForm = form
        if (form == null) {
            loadedPokemon = null
            forms = emptyList()
            statsByForm = emptyMap()
            isLoading = false
            return
        }
        val pokemon = baseFormOf(form)
        if (pokemon != loadedPokemon) load(pokemon)
        if (loadedPokemon == pokemon && form !in forms) formToShowInstead = pokemon
    }

    private suspend fun load(pokemon: PokemonName) {
        loadedPokemon = null
        forms = emptyList()
        statsByForm = emptyMap()
        isLoading = true
        val megaCandidates = MEGA_SUFFIXES.map { PokemonName(pokemon.normalized.value + it) }
        pokemonBaseStatsRepository.getBaseStats(listOf(pokemon) + megaCandidates).onRight { stats ->
            // the repository gives zeros for a pokemon it doesn't know
            val knownStats = stats.filterValues { it.hp > 0 }
            statsByForm = knownStats
            forms = listOf(pokemon) + megaCandidates.filter { it.normalized in knownStats }
            loadedPokemon = pokemon
        }
        isLoading = false
    }
}
