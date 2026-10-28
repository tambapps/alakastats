package com.tambapps.pokemon.alakastats.infrastructure.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tambapps.pokemon.PokemonNormalizer
import com.tambapps.pokemon.PokemonNormalizer.normalize
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.domain.error.NetworkError
import com.tambapps.pokemon.alakastats.domain.model.GameEndReason
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.transformer.toDomain
import com.tambapps.pokemon.alakastats.domain.transformer.toEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.OpenTeamSheetEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.PlayerEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.ReplayAnalyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamPreviewEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamPreviewPokemonEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.BestOfEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.MegaEvolutionEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TerastallizationEntity
import com.tambapps.pokemon.sd.replay.log.visitor.OtsPokemon
import com.tambapps.pokemon.sd.replay.log.visitor.SdReplayLogVisitor
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class ReplayAnalyticsService(
    private val httpClient: HttpClient,
) {

    suspend fun fetch(sdReplayUrl: String): Either<DomainError, ReplayAnalytics> {
        return try {
            val jsonReplayUrl = jsonReplayUrl(sdReplayUrl)
            val reference = jsonReplayUrl.substring(jsonReplayUrl.lastIndexOf('/') + 1).removeSuffix(".json")
            val rawReplay = httpClient.get(jsonReplayUrl).body<RawSdReplay>()

            val visitor = ReplayAnalyticsBuilderVisitor(rawReplay.players)
            visitor.visitLogs(rawReplay.logs)
            val players = visitor.players
            ReplayAnalyticsEntity(
                players = players,
                uploadTime = rawReplay.uploadTime,
                format = rawReplay.formatId,
                rating = rawReplay.rating,
                version = "1.0",
                nextBattleRef = visitor.nextBattleRef,
                winner = visitor.winner,
                url = jsonReplayUrl,
                reference = reference,
                notes = null,
                bestOf = visitor.bestOf,
                turns = visitor.turns,
                pickCount = visitor.pickCount,
                endReason = visitor.endReason?.name,
            ).toDomain().right()
        } catch (e: Exception) {
            // could have better error handling but MEH
            NetworkError(
                message = e.message ?: "Failed to fetch replay",
                cause = e
            ).left()
        }
    }

    /**
     * The raw protocol log of the replay, to play it back in the app
     */
    suspend fun fetchLog(sdReplayUrl: String): Either<DomainError, String> {
        return try {
            httpClient.get(jsonReplayUrl(sdReplayUrl)).body<RawSdReplay>().logs.right()
        } catch (e: Exception) {
            NetworkError(
                message = e.message ?: "Failed to fetch replay",
                cause = e
            ).left()
        }
    }

    private fun jsonReplayUrl(originUrl: String): String {
        var result = originUrl
        val qpBeginningIndex = originUrl.indexOf(',')
        if (qpBeginningIndex > 0) {
            result = result.substring(0, qpBeginningIndex)
        }
        return if (result.endsWith(".json")) result
        else "$result.json"
    }
}

@Serializable
private data class RawSdReplay(
    @SerialName("formatid")
    val formatId: String,
    val players: List<String>,
    @SerialName("uploadtime")
    val uploadTime: Long,
    val rating: Int?,
    @SerialName("log")
    val logs: String
)

// doing this because some events pokemon name don't contain forms, it is the base name but I want it to be the name of the selection
private fun getActualPokemonName(player: PlayerBuilderEntityBuilder, pokemonName: String): String {
    return player.selection.find { PokemonNormalizer.baseMatches(it, pokemonName) } ?: pokemonName
}

internal class ReplayAnalyticsBuilderVisitor(
    private val playerNames: List<String>
): SdReplayLogVisitor {

    private companion object {
        const val STRUGGLE = "struggle"
        const val FORFEIT_MESSAGE_SUFFIX = " forfeited."
        const val TIMER_MESSAGE_SUFFIX = " lost due to inactivity."
    }
    private val playerBuilders = mutableMapOf<String, PlayerBuilderEntityBuilder>()
    // the Pokémon in each slot (e.g. p1a), as the faint logs give its nickname
    private val activePokemons = mutableMapOf<String, String>()

    val players get() = playerBuilders.values.map { it.build() }

    var nextBattleRef: String? = null
        private set
    var bestOf: BestOfEntity? = null
        private set
    var winner = ""
        private set
    var turns = 0
        private set
    var pickCount: Int? = null
        private set
    var endReason: GameEndReason? = null
        private set

    override fun visitMoveLog(
        sourcePokemonSlot: String,
        sourcePokemonName: String,
        moveName: String,
        targetPokemonSlot: String?,
        targetPokemonName: String?,
        isSpread: Boolean,
        isStill: Boolean,
        additionalInfo: String
    ) {
        val builder = getPlayer(sourcePokemonSlot)
        if (moveName != STRUGGLE) {
            builder.moveUsage(sourcePokemonName, moveName)
        }
    }

    override fun visitPokeLog(
        playerSlot: String,
        pokemonName: String,
        level: Int?,
        gender: String?
    ) {
        val builder = getPlayer(playerSlot)
        builder.teamPreviewPokemon(TeamPreviewPokemonEntity(
            pokemonName,
            level ?: 50
        ))
    }

    override fun visitNextBattleUhtmlLog(name: String, content: String, nextBattleRef: String) {
        this.nextBattleRef = nextBattleRef
    }

    override fun visitBestOfUhtmlLog(name: String, content: String, gameNumber: Int, bestOf: Int, seriesRef: String) {
        this.bestOf = BestOfEntity(gameNumber, bestOf, seriesRef)
    }

    override fun visitRatingUpdateRawLog(
        content: String,
        playerName: String,
        beforeElo: Int,
        afterElo: Int
    ) {
        val builder = getPlayerByName(playerName)
        builder.beforeElo = beforeElo
        builder.afterElo = afterElo
    }

    override fun visitDragLog(pokemonSlot: String, pokemonName: String, hpPercentage: Int?) {
        val player = getPlayer(pokemonSlot)
        val actualPokemonName = getActualPokemonName(player, pokemonName)
        player.selection(actualPokemonName)
        activePokemons[pokemonSlot] = actualPokemonName
    }

    override fun visitFaintLog(pokemonSlot: String) {
        val pokemonName = activePokemons[pokemonSlot] ?: return
        getPlayer(pokemonSlot).faint(pokemonName)
    }

    override fun visitTurnLog(turnNumber: Int) {
        turns = turnNumber
    }

    override fun visitTeamPreviewLog(teamSize: Int) {
        pickCount = teamSize
    }

    override fun visitMessageLog(message: String) {
        when {
            message.endsWith(FORFEIT_MESSAGE_SUFFIX) -> endReason = GameEndReason.FORFEIT
            message.endsWith(TIMER_MESSAGE_SUFFIX) -> endReason = GameEndReason.TIMER
        }
    }

    override fun visitSwitchLog(pokemonSlot: String, pokemonName: String, hpPercentage: Int?) =
        visitDragLog(pokemonSlot, pokemonName, hpPercentage)

    override fun visitTerastallizeLog(pokemonSlot: String, pokemonName: String, teraType: String) {
        val player = getPlayer(pokemonSlot)
        player.terastallization = TerastallizationEntity(
            pokemon = getActualPokemonName(player, pokemonName),
            type = teraType.uppercase()
        )
    }

    override fun visitMegaLog(pokemonSlot: String, pokemonName: String, item: String) {
        val player = getPlayer(pokemonSlot)
        player.megaEvolution = MegaEvolutionEntity(
            pokemon = getActualPokemonName(player, pokemonName),
            item = formatPokemonTrait(item)
        )
    }

    override fun visitWinLog(winner: String) {
        this.winner = winner
    }

    override fun visitShowTeamLog(playerSlot: String, otsPokemons: List<OtsPokemon>) {
        getPlayer(playerSlot).ots = OpenTeamSheetEntity(
            pokemons = otsPokemons.map { it.toEntity() }
        )
    }

    private fun getPlayer(playerOrPokemonSlot: String): PlayerBuilderEntityBuilder {
        val playerName =
            if (playerOrPokemonSlot.startsWith("p2")) playerNames.last()
            else playerNames.first()
        return playerBuilders.getOrPut(playerName) {
            PlayerBuilderEntityBuilder(playerName)
        }
    }

    private fun getPlayerByName(name: String): PlayerBuilderEntityBuilder {
        return if (name.lowercase() == playerNames.first().lowercase()) playerBuilders.getValue(playerNames.first())
        else playerBuilders.getValue(playerNames.last())
    }

    override fun formatPokemonName(name: String) = normalize(name).let {
        if (it.endsWith("-*")) it.dropLast(2) // e.g. greninja-*
        else it
    }

    override fun formatPokemonTrait(name: String) = normalize(name)
}


private data class PlayerBuilderEntityBuilder(
    var name: String = "<none>",
    var selection: MutableList<String> = mutableListOf(),
    var beforeElo: Int? = null,
    var afterElo: Int? = null,
    var terastallization: TerastallizationEntity? = null,
    var megaEvolution: MegaEvolutionEntity? = null,
    var ots: OpenTeamSheetEntity? = null,
    val movesUsage: MutableMap<String, MutableMap<String, Int>> = mutableMapOf(),
) {

    private val teamPreviewPokemons = mutableListOf<TeamPreviewPokemonEntity>()
    private val fainted = mutableListOf<String>()

    fun teamPreviewPokemon(pokemon: TeamPreviewPokemonEntity) = teamPreviewPokemons.add(pokemon)

    fun selection(pokemon: String) {
        if (!selection.contains(pokemon)) {
            selection.add(pokemon)
        }
    }

    // once, even when revived and fainted again
    fun faint(pokemon: String) {
        if (!fainted.contains(pokemon)) {
            fainted.add(pokemon)
        }
    }

    fun moveUsage(pokemonName: String, moveName: String) {
        val movesCount = movesUsage.getOrPut(getActualPokemonName(this, pokemonName)) { mutableMapOf() }
        movesCount[moveName] = movesCount.getOrPut(moveName) { 0 } + 1
    }

    fun build() = PlayerEntity(
        name = name,
        teamPreview = TeamPreviewEntity(teamPreviewPokemons),
        selection = selection.toList(),
        beforeElo = beforeElo,
        afterElo = afterElo,
        terastallization = terastallization,
        megaEvolution = megaEvolution,
        ots = ots,
        movesUsage = movesUsage.toMap(),
        fainted = fainted.toList(),
    )

}
