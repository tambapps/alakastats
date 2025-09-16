package com.tambapps.pokemon.alakastats.domain.model

import com.tambapps.pokemon.PokeType

fun Teamlytics.getPlayers(replay: ReplayAnalytics): Pair<Player, Player> =
    if (sdNames.contains(replay.player1.name)) replay.player1 to replay.player2
    else replay.player2 to replay.player1

fun Teamlytics.getGameOutput(replay: ReplayAnalytics) = when {
    sdNames.contains(replay.winner) -> GameOutput.WIN
    sdNames.contains(replay.looser) -> GameOutput.LOOSE
    else -> GameOutput.UNKNOWN
}

enum class GameOutput {
    WIN, LOOSE, UNKNOWN
}
data class ReplayAnalytics(
    val players: List<Player>,
    val uploadTime: Long,
    val format: String,
    val rating: Int?,
    val version: String,
    val winner: String?,
    val nextBattleRef: String?,
) {
    val looser get() = when {
        winner == player1.name -> player2.name
        winner == player2.name -> player1.name
        else -> null
    }

    val player1 get() = players[0]
    val player2 get() = players[1]
}


data class Terastallization(
    val pokemon: String,
    val type: PokeType
)

data class OpenTeamSheet(
    val pokemons: List<OtsPokemon>,
)

data class OtsPokemon(
    val name: String,
    val item: String,
    val ability: String,
    val moves: List<String>,
    val level: Int,
    val teraType: PokeType?
)

data class TeamPreview(
    val pokemons: List<TeamPreviewPokemon>,
)

data class TeamPreviewPokemon(
    val name: String,
    val level: Int?
)

data class Player(
    val name: String,
    val teamPreview: TeamPreview,
    val selection: List<String>,
    val beforeElo: Int?,
    val afterElo: Int?,
    val terastallization: Terastallization?,
    val ots: OpenTeamSheet?,
    val movesUsage: Map<String, Map<String, Int>>
) {
    val lead get() = selection.take(2)
}