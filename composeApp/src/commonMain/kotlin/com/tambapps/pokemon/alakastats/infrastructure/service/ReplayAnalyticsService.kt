package com.tambapps.pokemon.alakastats.infrastructure.service

import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.transformer.OtsPokemonTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.ReplayAnalyticsTransformer
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.OpenTeamSheetEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.PlayerEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.ReplayAnalyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamPreviewEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamPreviewPokemonEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TerastallizationEntity
import com.tambapps.pokemon.alakastats.util.PokemonNormalizer.normalize
import com.tambapps.pokemon.sd.replay.log.visitor.OtsPokemon
import com.tambapps.pokemon.sd.replay.log.visitor.SdReplayLogVisitor
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class ReplayAnalyticsService(
    private val httpClient: HttpClient,
    private val otsPokemonTransformer: OtsPokemonTransformer,
    private val replayAnalyticsTransformer: ReplayAnalyticsTransformer
    ) {

    // TODO throw/handle exception
    suspend fun fetch(sdReplayUrl: String): ReplayAnalytics {
        val jsonReplayUrl =
            if (sdReplayUrl.endsWith(".json")) sdReplayUrl
        else "$sdReplayUrl.json"
        val rawReplay = httpClient.get(jsonReplayUrl).body<RawSdReplay>()

        val visitor = ReplayAnalyticsBuilderVisitor(otsPokemonTransformer, rawReplay.players)
        visitor.visitLogs(rawReplay.logs)
        val players = visitor.players
        return ReplayAnalyticsEntity(
            players = players,
            uploadTime = rawReplay.uploadTime,
            format = rawReplay.formatId,
            rating = rawReplay.rating,
            version = "1.0",
            nextBattleRef = visitor.nextBattleRef,
            winner = visitor.winner
        ).let(replayAnalyticsTransformer::toDomain)
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

private class ReplayAnalyticsBuilderVisitor(
    private val otsPokemonTransformer: OtsPokemonTransformer,
    private val playerNames: List<String>
): SdReplayLogVisitor {

    private companion object {
        const val STRUGGLE = "struggle"
    }
    private val playerBuilders = mutableMapOf<String, PlayerBuilderEntityBuilder>()

    val players get() = playerBuilders.values.map { it.build() }

    var nextBattleRef: String? = null
        private set
    var winner = ""
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
        getPlayer(pokemonSlot).selection(pokemonName)
    }

    override fun visitSwitchLog(pokemonSlot: String, pokemonName: String, hpPercentage: Int?) =
        visitDragLog(pokemonSlot, pokemonName, hpPercentage)

    override fun visitTerastallizeLog(pokemonSlot: String, pokemonName: String, teraType: String) {
        getPlayer(pokemonSlot).terastallization = TerastallizationEntity(
            pokemon = pokemonName,
            type = PokeType.valueOf(teraType.uppercase())
        )
    }

    override fun visitWinLog(winner: String) {
        this.winner = winner
    }

    override fun visitShowTeamLog(playerSlot: String, otsPokemons: List<OtsPokemon>) {
        getPlayer(playerSlot).ots = OpenTeamSheetEntity(
            pokemons = otsPokemons.map(otsPokemonTransformer::toEntity)
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
        return if (name.lowercase() == playerNames.first()) playerBuilders.getValue(playerNames.first())
        else playerBuilders.getValue(playerNames.last())
    }

    override fun formatPokemonName(name: String) = normalize(name)

    override fun formatPokemonTrait(name: String) = normalize(name)
}


private data class PlayerBuilderEntityBuilder(
    var name: String = "<none>",
    var selection: MutableList<String> = mutableListOf(),
    var beforeElo: Int? = 0,
    var afterElo: Int? = 0,
    var terastallization: TerastallizationEntity? = null,
    var ots: OpenTeamSheetEntity? = null,
    val movesUsage: MutableMap<String, MutableMap<String, Int>> = mutableMapOf(),
) {

    private val teamPreviewPokemons = mutableListOf<TeamPreviewPokemonEntity>()

    fun teamPreviewPokemon(pokemon: TeamPreviewPokemonEntity) = teamPreviewPokemons.add(pokemon)

    fun selection(pokemon: String) {
        if (!selection.contains(pokemon)) {
            selection.add(pokemon)
        }
    }

    fun moveUsage(pokemonName: String, moveName: String) {
        val movesCount = movesUsage.getOrPut(pokemonName) { mutableMapOf() }
        movesCount[moveName] = movesCount.getOrPut(moveName) { 0 } + 1
    }

    fun build() = PlayerEntity(
        name = name,
        teamPreview = TeamPreviewEntity(teamPreviewPokemons),
        selection = selection.toList(),
        beforeElo = beforeElo,
        afterElo = afterElo,
        terastallization = terastallization,
        ots = ots,
        movesUsage = movesUsage.toMap()
    )
}
