package com.tambapps.pokemon.alakastats.ui.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.tambapps.pokemon.PokemonName

data class ReplayFilters(
    val opponentTeam: PokemonsFilters = PokemonsFilters(),
    val yourSelection: PokemonsFilters = PokemonsFilters(),
) {
    fun hasAny() = opponentTeam.isNotEmpty() || yourSelection.isNotEmpty()
}

data class PokemonsFilters(
    val pokemons: SnapshotStateList<PokemonFilter> = mutableStateListOf()
) {
    fun isNotEmpty() = pokemons.isNotEmpty()
}

data class PokemonFilter(
    val name: PokemonName,
    val asLead: Boolean
    // maybe I'll add something else later
)
