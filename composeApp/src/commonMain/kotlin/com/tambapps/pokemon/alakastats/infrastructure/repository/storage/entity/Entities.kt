package com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity

import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.pokepaste.parser.PokePaste
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class TeamlyticsEntity(
    val id: Uuid,
    val name: String,
    val pokePaste: String,
    val replays: List<ReplayAnalyticsEntity>,
    val sdNames: List<String>
)

@Serializable
data class ReplayAnalyticsEntity(
    val replay: SdReplayEntity,
)

@Serializable
data class TerastallizationEntity(
    val pokemon: String,
    val type: PokeType
)

@Serializable
data class SdReplayEntity(
    val players: List<PlayerEntity>,
    val uploadTime: Long,
    val format: String,
    val rating: Int?,
    val parserVersion: String?,
    val winner: String?,
    val nextBattle: String?,
)

@Serializable
data class OpenTeamSheetEntity(
    val pokemons: List<OtsPokemonEntity>,
)

@Serializable
data class OtsPokemonEntity(
    val name: String,
    val item: String,
    val ability: String,
    val moves: List<String>,
    val level: Int,
    val teraType: PokeType?
)

@Serializable
data class TeamPreviewEntity(
    val pokemons: List<TeamPreviewPokemonEntity>,
)

@Serializable
data class TeamPreviewPokemonEntity(
    val name: String,
    val level: Int?
)

@Serializable
data class PlayerEntity(
    val name: String,
    val teamPreview: TeamPreviewEntity,
    val selection: List<String>,
    val beforeElo: Int?,
    val afterElo: Int?,
    val terastallization: TerastallizationEntity?,
    val ots: OpenTeamSheetEntity?,
    val movesUsage: Map<String, Map<String, Int>>
)