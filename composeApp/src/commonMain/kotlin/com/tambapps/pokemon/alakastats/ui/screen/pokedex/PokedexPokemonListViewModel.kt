package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.alakastats.domain.model.Pokedex
import com.tambapps.pokemon.alakastats.domain.model.PokedexSpecies
import com.tambapps.pokemon.alakastats.domain.model.foldName
import com.tambapps.pokemon.alakastats.domain.repository.PokedexRepository
import com.tambapps.pokemon.alakastats.ui.model.PokedexPokemonSort
import com.tambapps.pokemon.alakastats.ui.model.sortedBy

/**
 * The Pokémon of the Pokédex, searched by name and sorted by name or speed. The Megas are only listed on demand: they're
 * the forms of their Pokémon, on its sheet
 */
class PokedexPokemonListViewModel(
    private val pokedexRepository: PokedexRepository,
) : ViewModel() {

    var query by mutableStateOf("")
    var sort by mutableStateOf(PokedexPokemonSort())
    // the Megas are on their Pokémon's sheet: they're only listed on demand
    var includeMegas by mutableStateOf(false)
    // the type the Pokémon must have, null for all of them
    var type by mutableStateOf<PokeType?>(null)
    var isLoading by mutableStateOf(true)
        private set
    var loadingFailed by mutableStateOf(false)
        private set
    private var pokedex by mutableStateOf<Pokedex?>(null)

    /** The Pokémon matching the query and of the type, in the order of the sort */
    val pokemons: List<PokedexSpecies> by derivedStateOf {
        val pokedex = pokedex ?: return@derivedStateOf emptyList()
        matching(pokedex, query, includeMegas)
            .filter { pokemon -> type.let { it == null || it in pokemon.types } }
            .sortedBy(sort)
    }

    suspend fun load() {
        if (pokedex != null) return
        isLoading = true
        pokedexRepository.getPokedex()
            .onRight { pokedex = it }
            .onLeft { loadingFailed = true }
        isLoading = false
    }
}

/**
 * The Pokémon whose English or French name contains the query (ignoring the case, accents and punctuation), all of them
 * for no query. With none, the ones whose name the query is close to, for a typo
 *
 * @param includeMegas whether the Megas are among them
 */
internal fun matching(pokedex: Pokedex, query: String, includeMegas: Boolean = false): List<PokedexSpecies> {
    val pokemons = pokedex.species.filter { (includeMegas || !it.isMega) && !it.isBattleOnly }
    val folded = foldName(query)
    if (folded.isEmpty()) return pokemons
    val containing = pokemons.filter { folded in foldName(it.name.en) || folded in foldName(it.name.fr) }
    if (containing.isNotEmpty()) return containing
    return pokedex.searchByName(query, limit = 10).filterIsInstance<PokedexSpecies>().filter { (includeMegas || !it.isMega) && !it.isBattleOnly }
}
