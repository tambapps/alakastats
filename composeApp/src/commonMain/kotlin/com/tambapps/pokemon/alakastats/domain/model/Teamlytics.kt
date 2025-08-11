package com.tambapps.pokemon.alakastats.domain.model

import com.tambapps.pokemon.pokepaste.parser.PokePaste
import com.tambapps.pokemon.sd.replay.parser.SdReplay
import kotlin.uuid.Uuid

data class Teamlytics(
    val id: Uuid,
    val name: String,
    val pokePaste: PokePaste,
    val replays: List<ReplayAnalytics>,
    val sdNames: List<String>
) {
    val winRate get() = computeWinRate(sdNames, replays)
}

data class TeamlyticsPreview(
    val id: Uuid,
    val name: String,
    val sdNames: List<String>,
    val pokemons: List<String>,
    val nbReplays: Int,
    val winrate: Int
)

fun computeWinRate(sdNames: List<String>, replays: List<ReplayAnalytics>): Int {
    if (replays.size == 0) return 0
    val nbGames = replays.size
    val wonGames = replays.count { replay ->
        replay.replay.winner?.let(sdNames::contains) ?: false
    }
    return nbGames * 100 / wonGames
}

data class ReplayAnalytics(
    val replay: SdReplay,
)