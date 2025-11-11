package com.tambapps.pokemon.alakastats.domain.model

import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.TeraType
import com.tambapps.pokemon.pokepaste.parser.PokePaste

fun Teamlytics.getOpponentPlayer(replay: ReplayAnalytics) =
    if (sdNames.contains(replay.player1.name)) replay.player2
    else replay.player1

fun Teamlytics.getYouPlayer(replay: ReplayAnalytics) =
    if (sdNames.contains(replay.player1.name)) replay.player1
    else replay.player2

fun Teamlytics.getPlayers(replay: ReplayAnalytics): Pair<Player, Player> =
    if (sdNames.contains(replay.player1.name)) replay.player1 to replay.player2
    else replay.player2 to replay.player1

fun Teamlytics.getGameOutput(replay: ReplayAnalytics) = when {
    sdNames.contains(replay.winner) -> GameOutput.WIN
    sdNames.contains(replay.looser) -> GameOutput.LOOSE
    else -> GameOutput.UNKNOWN
}

class TeamlyticsContext(val team: Teamlytics) {
    val ReplayAnalytics.gameOutput: GameOutput
        get() = team.getGameOutput(this)

    val ReplayAnalytics.youPlayer: Player
        get() = team.getYouPlayer(this)

    val ReplayAnalytics.opponentPlayer: Player
        get() = team.getOpponentPlayer(this)


}

inline fun <R> Teamlytics.withContext(block: TeamlyticsContext.() -> R): R {
    return TeamlyticsContext(this).block()
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
    val url: String?,
    val reference: String,
    val nextBattleRef: String?,
    val notes: String?,
) {
    val looser get() = when (winner) {
        player1.name -> player2.name
        player2.name -> player1.name
        else -> null
    }

    val player1 get() = players[0]
    val player2 get() = players[1]

    fun hasWon(player: Player) = winner == player.name

    // complete information of replay from another one. Useful when reloading replys
    fun completedWith(oldReplay: ReplayAnalytics) = copy(
        notes = notes ?: oldReplay.notes,
        players = players.mapIndexed { index, player ->
            player.copy(
                beforeElo = player.beforeElo ?: oldReplay.players.getOrNull(index)?.beforeElo,
                afterElo = player.afterElo ?: oldReplay.players.getOrNull(index)?.afterElo
            )
        }
    )
}

fun List<ReplayAnalytics>.withComputedElo() = map { replay ->
    if (replay.player1.beforeElo != null) return@map replay
    val next = findNextMatchWithElo(replay) ?: return@map replay

    return@map replay.copy(
        players = replay.players.mapIndexed { index, player ->
            val nextPlayer = next.players.getOrNull(index) ?: player
            player.copy(
                beforeElo = nextPlayer.beforeElo
            )
        }
    )
}

private fun List<ReplayAnalytics>.findNextMatchWithElo(replay: ReplayAnalytics): ReplayAnalytics? {
    val visitedReplays = mutableSetOf<ReplayAnalytics>()
    var r: ReplayAnalytics = replay

    while (r.nextBattleRef != null && !visitedReplays.contains(r)) {
        visitedReplays.add(r)
        val next = find { it.reference == r.nextBattleRef } ?: return null
        if (next.player1.beforeElo != null) {
            return next
        }
        r = next
    }
    return null
}

data class Terastallization(
    val pokemon: PokemonName,
    val type: TeraType
)

data class OpenTeamSheet(
    val pokemons: List<OtsPokemon>,
) {

    fun toPokepaste(): PokePaste {
        val pastePokemons = pokemons.map { p ->
            Pokemon(
                name = p.name,
                surname = null,
                gender = null,
                nature = null,
                item = p.item,
                shiny = false,
                happiness = 0,
                ability = p.ability,
                teraType = p.teraType,
                level = 50,
                moves = p.moves,
                ivs = PokeStats.default(0),
                evs = PokeStats.default(0),
            )
        }
        return PokePaste(pastePokemons, isOts = true)
    }
}

data class OtsPokemon(
    val name: PokemonName,
    val item: String,
    val ability: String,
    val moves: List<String>,
    val level: Int,
    val teraType: TeraType?
)

data class TeamPreview(
    val pokemons: List<TeamPreviewPokemon>,
)

data class TeamPreviewPokemon(
    val name: PokemonName,
    val level: Int?
)

data class Player(
    val name: String,
    val teamPreview: TeamPreview,
    val selection: List<PokemonName>,
    val beforeElo: Int?,
    val afterElo: Int?,
    val terastallization: Terastallization?,
    val ots: OpenTeamSheet?,
    val movesUsage: Map<PokemonName, Map<String, Int>>
) {
    // sorted to always have the same order
    val lead get() = selection.take(2).sortedBy { it.value }

    fun hasSelected(pokemonName: PokemonName) = selection.any { pokemonName.matches(it) }

    fun hasTerastallized(pokemonName: PokemonName) = terastallization?.pokemon?.matches(pokemonName) == true

    fun isLead(pokemonName: PokemonName) = lead.any { pokemonName.matches(it) }
}