package com.tambapps.pokemon.alakastats.ui.model

import com.tambapps.pokemon.PokemonName

data class ReplayFilters(
    val opponentTeam: List<PokemonFilter> = listOf(),
    val opponentSelection: List<PokemonFilter> = listOf(),
    val yourSelection: List<PokemonFilter> = listOf(),
) {
    fun hasAny() = opponentTeam.isNotEmpty() || opponentSelection.isNotEmpty() || yourSelection.isNotEmpty()
}

data class PokemonFilter(
    val name: PokemonName,
    val asLead: Boolean
)
