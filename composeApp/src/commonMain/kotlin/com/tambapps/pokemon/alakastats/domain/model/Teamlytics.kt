package com.tambapps.pokemon.alakastats.domain.model

import com.tambapps.pokemon.pokepaste.parser.PokePaste
import com.tambapps.pokemon.sd.replay.parser.SdReplay
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class Teamlytics(
    val id: Uuid,
    val name: String,
    val pokePaste: PokePaste,
    val replays: List<ReplayAnalytics>,
    val sdNames: List<String>,
    val lastUpdatedAt: Instant
) {
    val winRate get() = computeWinRate(sdNames, replays)
}

data class TeamlyticsPreview(
    val id: Uuid,
    val name: String,
    val sdNames: List<String>,
    val pokemons: List<String>,
    val nbReplays: Int,
    val winrate: Int,
    val lastUpdatedAt: Instant
)

fun computeWinRate(sdNames: List<String>, replays: List<ReplayAnalytics>): Int {
    if (replays.isEmpty()) return 0
    val nbGames = replays.size
    val wonGames = replays.count { replay ->
        replay.replay.winner?.let(sdNames::contains) ?: false
    }
    return nbGames * 100 / wonGames
}

data class ReplayAnalytics(
    val replay: SdReplay,
)