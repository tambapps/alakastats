package com.tambapps.pokemon.alakastats.domain.model

import com.tambapps.pokemon.PokemonName

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
 * @param team the 6 Pokémon of the team preview
 * @param brought the Pokémon that went on the field, in the order they did: the first 2 are the lead
 */
data class ShowdownReplayPlayer(
    val name: UserName,
    val team: List<PokemonName>,
    val brought: List<PokemonName>,
    val beforeElo: Int?,
    val afterElo: Int?,
) {
    val lead get() = brought.take(2)
}

/**
 * The filters of the saved Showdown replays. The criteria about a player must all be met by the same one, e.g. "Whimsicott
 * in the team" and "the username contains alaka" keeps the replays where alaka played Whimsicott
 *
 * @param teamPokemons Pokémon all in a player's team
 * @param broughtPokemons Pokémon all brought by a player
 * @param userName a part of a player's name, whatever its case
 * @param minRating the battle's minimum rating: the replays without one are left out
 */
data class ShowdownReplayFilters(
    val teamPokemons: List<PokemonName> = emptyList(),
    val broughtPokemons: List<PokemonName> = emptyList(),
    val userName: String? = null,
    val minRating: Int? = null,
) {
    val isEmpty get() = teamPokemons.isEmpty() && broughtPokemons.isEmpty() && userName.isNullOrBlank() && minRating == null

    private val hasPlayerCriteria get() = teamPokemons.isNotEmpty() || broughtPokemons.isNotEmpty() || !userName.isNullOrBlank()

    fun matches(replay: ShowdownReplaySummary): Boolean {
        if (minRating != null && (replay.rating ?: return false) < minRating) return false
        return !hasPlayerCriteria || replay.players.any(::matches)
    }

    private fun matches(player: ShowdownReplayPlayer) =
        teamPokemons.all { pokemon -> player.team.any { it.baseMatches(pokemon) } }
            && broughtPokemons.all { pokemon -> player.brought.any { it.baseMatches(pokemon) } }
            && (userName.isNullOrBlank() || player.name.value.contains(userName.trim(), ignoreCase = true))
}
