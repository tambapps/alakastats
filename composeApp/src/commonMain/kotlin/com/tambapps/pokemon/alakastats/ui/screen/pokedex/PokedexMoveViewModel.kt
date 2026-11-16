package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.tambapps.pokemon.MoveName
import com.tambapps.pokemon.alakastats.domain.model.PokedexMove
import com.tambapps.pokemon.alakastats.domain.model.PokedexSpecies
import com.tambapps.pokemon.alakastats.domain.repository.PokedexRepository
import com.tambapps.pokemon.alakastats.ui.model.PokedexPokemonSort
import com.tambapps.pokemon.alakastats.ui.model.sortedBy

/**
 * The sheet of a move: what it is, and the Pokémon learning it, filtered and sorted like the Pokémon's list
 */
class PokedexMoveViewModel(
    private val pokedexRepository: PokedexRepository,
) : ViewModel() {

    var move by mutableStateOf<PokedexMove?>(null)
        private set
    var isLoading by mutableStateOf(true)
        private set
    var loadingFailed by mutableStateOf(false)
        private set
    // the move asked isn't one of the Pokédex (e.g. in an URL)
    var isUnknown by mutableStateOf(false)
        private set

    var sort by mutableStateOf(PokedexPokemonSort())
    // only the Pokémon of the move's type, which get the same-type attack bonus
    var stabOnly by mutableStateOf(false)
    // the Megas learn the moves of their base form: they're only listed on demand
    var includeMegas by mutableStateOf(false)
    private var learners by mutableStateOf(emptyList<PokedexSpecies>())

    /** The Pokémon learning the move, without the Megas, all of them whatever the filters */
    val learnerCount: Int by derivedStateOf { learners.count { !it.isMega } }

    /** The Pokémon learning the move matching the filters, in the order of the sort */
    val pokemons: List<PokedexSpecies> by derivedStateOf {
        learners.filter { (includeMegas || !it.isMega) && (!stabOnly || isStab(it)) }.sortedBy(sort)
    }

    /** Whether the Pokémon gets the same-type attack bonus with the move */
    fun isStab(pokemon: PokedexSpecies) = move?.let { it.type in pokemon.types } == true

    suspend fun load(id: String) {
        if (move?.id == id) return
        isLoading = true
        isUnknown = false
        pokedexRepository.getPokedex()
            .onRight { pokedex ->
                val found = pokedex.move(MoveName(id))
                move = found
                learners = found?.let(pokedex::learners).orEmpty().filterNot { it.isBattleOnly }
                isUnknown = found == null
            }
            .onLeft { loadingFailed = true }
        isLoading = false
    }
}
