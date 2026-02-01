package com.tambapps.pokemon.alakastats.domain.model

import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.pokepaste.parser.PokePaste
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class Teamlytics(
    val id: Uuid,
    val name: String,
    val pokePaste: PokePaste,
    val replays: List<ReplayAnalytics>,
    val sdNames: List<String>,
    val lastUpdatedAt: Instant,
    val notes: TeamlyticsNotes?,
    val matchupNotes: List<MatchupNotes>
) {
    val winRate get() = computeWinRatePercentage(sdNames, replays)

}

data class MatchupNotes(
    val id: Uuid,
    val name: String,
    val pokePaste: PokePaste?,
    val gamePlans: List<GamePlan>
)

data class GamePlan(
    val description: String,
    val composition: List<PokemonName>?, // the 4 pokemons to bring
    val exampleReplays: List<ReplayAnalytics>
)
data class TeamlyticsNotes(
    val teamNotes: String,
    val pokemonNotes: Map<PokemonName, String>
)

data class TeamlyticsPreview(
    val id: Uuid,
    val name: String,
    val sdNames: List<String>,
    val pokemons: List<PokemonName>,
    val nbReplays: Int,
    val winrate: Int,
    val lastUpdatedAt: Instant
)

fun Teamlytics.computeWinRatePercentage() = computeWinRatePercentage(sdNames, replays)

fun computeWinRatePercentage(sdNames: List<String>, replays: List<ReplayAnalytics>): Int {
    if (replays.isEmpty()) return 0
    val nbGames = replays.size
    val wonGames = replays.count { replay ->
        replay.winner?.let(sdNames::contains) ?: false
    }
    return wonGames * 100 / nbGames
}