package com.tambapps.pokemon.alakastats.ui.model

import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.Player
import com.tambapps.pokemon.alakastats.domain.model.UserName

data class ReplayFilters(
    val opponentTeam: List<PokemonFilter> = listOf(),
    val opponentSelection: List<PokemonFilter> = listOf(),
    val opponentUsernames: Set<UserName> = setOf(),
    val yourSelection: List<PokemonFilter> = listOf(),
) {
    fun hasAny() = opponentTeam.isNotEmpty() || opponentSelection.isNotEmpty() || yourSelection.isNotEmpty()
            || opponentUsernames.isNotEmpty()

    fun matches(opponentPlayer: Player, youPlayer: Player): Boolean = when {
        opponentTeam.isNotEmpty() && !teamMatches(opponentPlayer, opponentTeam) -> false
        opponentSelection.isNotEmpty() && !selectionMatches(opponentPlayer, opponentSelection) -> false
        opponentUsernames.isNotEmpty() && !opponentUsernames.any { it.matches(opponentPlayer.name) } -> false
        yourSelection.isNotEmpty() && !selectionMatches(youPlayer, yourSelection) -> false
        else -> true
    }


    private fun teamMatches(player: Player, pokemonFilters: List<PokemonFilter>): Boolean {
        for (pokemonFilter in pokemonFilters) {
            if (player.teamPreview.pokemons.none { it.name.matches(pokemonFilter.name) }) {
                return false
            }
            if (pokemonFilter.asLead && player.lead.none { it.matches(pokemonFilter.name) }) {
                return false
            }
        }
        return true
    }

    private fun selectionMatches(player: Player, pokemonFilters: List<PokemonFilter>): Boolean {
        for (pokemonFilter in pokemonFilters) {
            if (player.selection.none { it.matches(pokemonFilter.name) }) {
                return false
            }
            if (pokemonFilter.asLead && player.lead.none { it.matches(pokemonFilter.name) }) {
                return false
            }
        }
        return true
    }

}

data class PokemonFilter(
    val name: PokemonName,
    val asLead: Boolean
)
