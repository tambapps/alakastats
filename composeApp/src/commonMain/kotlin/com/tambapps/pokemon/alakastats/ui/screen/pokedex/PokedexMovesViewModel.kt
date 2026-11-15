package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.tambapps.pokemon.alakastats.domain.model.Pokedex
import com.tambapps.pokemon.alakastats.domain.model.PokedexMove
import com.tambapps.pokemon.alakastats.domain.repository.PokedexRepository
import com.tambapps.pokemon.alakastats.ui.model.PokedexMoveFilters
import com.tambapps.pokemon.alakastats.ui.model.PokedexMoveSort
import com.tambapps.pokemon.alakastats.ui.model.matching
import com.tambapps.pokemon.alakastats.ui.model.sortedBy

/**
 * The moves of the Pokédex, searched by name or description, filtered and sorted
 */
class PokedexMovesViewModel(
    private val pokedexRepository: PokedexRepository,
) : ViewModel() {

    var query by mutableStateOf("")
    var filters by mutableStateOf(PokedexMoveFilters())
    var sort by mutableStateOf(PokedexMoveSort())
    var isLoading by mutableStateOf(true)
        private set
    var loadingFailed by mutableStateOf(false)
        private set
    private var pokedex by mutableStateOf<Pokedex?>(null)

    val allMoves: List<PokedexMove> get() = pokedex?.moves.orEmpty()

    /** The moves matching the query and the filters, in the order of the sort */
    val moves: List<PokedexMove> by derivedStateOf {
        allMoves.matching(query).filter(filters::matches).sortedBy(sort)
    }

    // the values the moves have, for the filters to only propose them
    val priorities: List<Int> by derivedStateOf { allMoves.map { it.priority }.distinct().sortedDescending() }
    val pps: List<Int> by derivedStateOf { allMoves.map { it.pp }.distinct().sorted() }

    suspend fun load() {
        if (pokedex != null) return
        isLoading = true
        pokedexRepository.getPokedex()
            .onRight { pokedex = it }
            .onLeft { loadingFailed = true }
        isLoading = false
    }

    fun clearFilters() {
        filters = PokedexMoveFilters()
    }
}
