package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.tambapps.pokemon.alakastats.domain.model.Pokedex
import com.tambapps.pokemon.alakastats.domain.model.PokedexSpecies
import com.tambapps.pokemon.alakastats.domain.model.foldName
import com.tambapps.pokemon.alakastats.domain.repository.PokedexRepository

/**
 * The Pokémon of the Pokédex, searched by name and sorted by name. The Megas aren't listed: they're the forms of their
 * Pokémon, on its sheet
 */
class PokedexPokemonListViewModel(
    private val pokedexRepository: PokedexRepository,
) : ViewModel() {

    var query by mutableStateOf("")
    var descending by mutableStateOf(false)
    var isLoading by mutableStateOf(true)
        private set
    var loadingFailed by mutableStateOf(false)
        private set
    private var pokedex by mutableStateOf<Pokedex?>(null)

    /** The Pokémon matching the query, by name */
    val pokemons: List<PokedexSpecies> by derivedStateOf {
        val pokedex = pokedex ?: return@derivedStateOf emptyList()
        val order = compareBy<PokedexSpecies> { it.name.en }
        matching(pokedex, query).sortedWith(if (descending) order.reversed() else order)
    }

    suspend fun load() {
        if (pokedex != null) return
        isLoading = true
        pokedexRepository.getPokedex()
            .onRight { pokedex = it }
            .onLeft { loadingFailed = true }
        isLoading = false
    }

    fun toggleOrder() {
        descending = !descending
    }
}

/**
 * The Pokémon whose English or French name contains the query (ignoring the case, accents and punctuation), all of them
 * for no query. With none, the ones whose name the query is close to, for a typo
 */
internal fun matching(pokedex: Pokedex, query: String): List<PokedexSpecies> {
    val pokemons = pokedex.species.filterNot { it.isMega }
    val folded = foldName(query)
    if (folded.isEmpty()) return pokemons
    val containing = pokemons.filter { folded in foldName(it.name.en) || folded in foldName(it.name.fr) }
    if (containing.isNotEmpty()) return containing
    return pokedex.searchByName(query, limit = 10).filterIsInstance<PokedexSpecies>().filterNot { it.isMega }
}
