package com.tambapps.pokemon.alakastats.domain.model

import com.tambapps.pokemon.ItemName
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.ui.model.PokemonFilter

/**
 * What the app keeps of a public Showdown replay: enough to show and filter it, the whole log being fetched again from
 * Showdown to watch it
 *
 * @param id the replay's id on Showdown, e.g. gen9championsvgc2026regmc-2711595110
 * @param formatId the format's id on Showdown, e.g. gen9championsvgc2026regmc
 * @param uploadTime in seconds since the epoch
 * @param rating the battle's rating on the ladder, null when not rated
 */
data class ShowdownReplaySummary(
    val id: String,
    val formatId: String,
    val uploadTime: Long,
    val rating: Int?,
    val winner: UserName?,
    val players: List<ShowdownReplayPlayer>,
    val bestOf: BestOf?,
) {
    val url get() = "$SHOWDOWN_REPLAYS_URL/$id"
}

const val SHOWDOWN_REPLAYS_URL = "https://replay.pokemonshowdown.com"

/**
 * The replays with, for the games of a series played before its last one, the series' rating: Showdown rates a series
 * once, on its last game, the others having none. Without it, they'd be left out by a minimum Elo and sorted last
 */
fun List<ShowdownReplaySummary>.withSeriesRatings(): List<ShowdownReplaySummary> {
    val seriesRatings = mapNotNull { replay ->
        val rating = replay.rating ?: return@mapNotNull null
        replay.bestOf?.let { it.seriesRef to rating }
    }.toMap()
    return map { replay ->
        if (replay.rating != null) replay
        else replay.bestOf?.let { seriesRatings[it.seriesRef] }?.let { replay.copy(rating = it) } ?: replay
    }
}

/**
 * The saved replays of a format
 *
 * @param bytes the room they take, as saved
 */
data class ShowdownReplayStorage(val count: Int, val bytes: Long)

/**
 * @param team the 6 Pokémon of the team preview
 * @param items the item of each Pokémon of the team, in its order, when known: from the open team sheet, or the Mega
 * Stone of the Pokémon that Mega Evolved
 * @param brought the Pokémon that went on the field, in the order they did: the first 2 are the lead
 */
data class ShowdownReplayPlayer(
    val name: UserName,
    val team: List<PokemonName>,
    val items: List<ItemName?>,
    val brought: List<PokemonName>,
    val beforeElo: Int?,
    val afterElo: Int?,
) {
    val lead get() = brought.take(2)

    /** The item of each brought Pokémon, in the order of [brought], when known */
    val broughtItems: List<ItemName?>
        get() = brought.map { pokemon -> items.getOrNull(team.indexOfFirst { it.baseMatches(pokemon) }) }
}

/**
 * The filters of the saved Showdown replays. The criteria about a player must all be met by the same one, e.g. "Whimsicott
 * in the team" and "the username contains alaka" keeps the replays where alaka played Whimsicott
 *
 * @param teamPokemons Pokémon all in a player's team
 * @param broughtPokemons Pokémon all brought by a player, those as lead among the first 2 that went on the field
 * @param userName a part of a player's name, whatever its case
 * @param minRating the battle's minimum rating: the replays without one are left out
 * @param winnerOnly whether the player meeting the criteria must be the winner, e.g. to see what won
 */
data class ShowdownReplayFilters(
    val teamPokemons: List<PokemonName> = emptyList(),
    val broughtPokemons: List<PokemonFilter> = emptyList(),
    val userName: String? = null,
    val minRating: Int? = null,
    val winnerOnly: Boolean = false,
) {
    private val hasPlayerCriteria get() =
        teamPokemons.isNotEmpty() || broughtPokemons.isNotEmpty() || !userName.isNullOrBlank() || winnerOnly

    val isEmpty get() = !hasPlayerCriteria && minRating == null

    fun matches(replay: ShowdownReplaySummary): Boolean {
        if (minRating != null && (replay.rating ?: return false) < minRating) return false
        if (!hasPlayerCriteria) return true
        val players = if (winnerOnly) replay.players.filter { player -> replay.winner?.matches(player.name) == true }
        else replay.players
        return players.any(::matches)
    }

    private fun matches(player: ShowdownReplayPlayer) =
        teamPokemons.all { pokemon -> player.team.any { it.baseMatches(pokemon) } }
            && broughtPokemons.all { filter ->
                (if (filter.asLead) player.lead else player.brought).any { it.baseMatches(filter.name) }
            }
            && (userName.isNullOrBlank() || player.name.value.contains(userName.trim(), ignoreCase = true))
}
