package com.tambapps.pokemon.alakastats.domain.model

import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.pokepaste.parser.PokePaste

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
    val url: String?,
    val reference: String,
    val nextBattleRef: String?,
) {
    val looser get() = when {
        winner == player1.name -> player2.name
        winner == player2.name -> player1.name
        else -> null
    }

    val player1 get() = players[0]
    val player2 get() = players[1]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ReplayAnalytics

        return reference == other.reference
    }

    override fun hashCode(): Int {
        return reference.hashCode()
    }
}


data class Terastallization(
    val pokemon: String,
    val type: PokeType
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
                teraType = p.teraType ?: PokeType.UNKNOWN,
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