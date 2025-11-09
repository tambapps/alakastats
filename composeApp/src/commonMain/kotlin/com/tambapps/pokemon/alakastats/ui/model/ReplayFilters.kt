package com.tambapps.pokemon.alakastats.ui.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.tambapps.pokemon.PokemonName

data class ReplayFilters(
    val opponentTeamFilters: OpponentTeamFilters = OpponentTeamFilters(),
    val yourSelectionFilters: SnapshotStateList<PokemonName> = mutableStateListOf(),
) {
    fun hasAny() = opponentTeamFilters.isNotEmpty() || yourSelectionFilters.isNotEmpty()
}

data class OpponentTeamFilters(
    val team: SnapshotStateList<PokemonFilter> = mutableStateListOf()
) {
    fun isNotEmpty() = team.isNotEmpty()
}

data class PokemonFilter(
    val name: PokemonName,
    // maybe I'll add something else later
)
