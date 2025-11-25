package com.tambapps.pokemon.alakastats.infrastructure.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.raise.either
import com.tambapps.pokemon.alakastats.domain.error.JsonError
import com.tambapps.pokemon.alakastats.domain.error.LoadTeamError
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.transformer.ReplayAnalyticsTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.TeamlyticsTransformer
import com.tambapps.pokemon.Gender
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.TeraType
import com.tambapps.pokemon.alakastats.domain.model.withComputedElo
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.MatchupNotesEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.PssPokepaste
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.PssPokepastePokemon
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.PssStats
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.PssTeamlytics
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.ReplayAnalyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsNotesEntity
import com.tambapps.pokemon.pokepaste.parser.PokePaste
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock
import kotlin.uuid.Uuid

class TeamlyticsSerializer(
    private val transformer: TeamlyticsTransformer,
    private val replayTransformer: ReplayAnalyticsTransformer,
    private val json: Json,
    private val replayService: ReplayAnalyticsService
) {

    fun export(teamlytics: Teamlytics): ByteArray {
        return json.encodeToString(transformer.toEntity(teamlytics)).toByteArray()
    }

    suspend fun loadTeam(byteArray: ByteArray): Either<LoadTeamError, Teamlytics> = either {
        val jsonObject = decodeJsonObject(byteArray).mapLeft { LoadTeamError("Invalid save", it.cause) }
            .bind()
        val entity = if (jsonObject["saveName"] != null) loadFromPokeShowStatsSave(jsonObject).bind()
        else loadTeamFromJson(jsonObject).bind()
        val team = transformer.toDomain(entity)
        team.copy(
            replays = team.replays.withComputedElo()
        )
    }

    private suspend fun loadTeamFromJson(jsonObject: JsonObject): Either<LoadTeamError, TeamlyticsEntity> = either {
        val name = jsonAccess { jsonObject[TeamlyticsEntity::name.name]?.jsonPrimitive?.contentOrNull ?: "<no name>" }.bind()
        val pokepaste = jsonAccess { jsonObject[TeamlyticsEntity::pokePaste.name]?.jsonPrimitive?.contentOrNull }.bind()
            ?: ""
        val replays = jsonAccess { jsonObject[TeamlyticsEntity::replays.name]?.jsonArray }
            .flatMap { if (it != null) loadReplaysFromTeamlyticsReplaysArray(it) else Either.Right(listOf()) }
            .bind()
        val sdNames = jsonAccess { jsonObject[TeamlyticsEntity::sdNames.name]?.jsonArray?.map { it.jsonPrimitive.content } }.bind()
            ?: listOf()
        val notes = jsonAccess { jsonObject[TeamlyticsEntity::notes.name]?.let { json.decodeFromJsonElement<TeamlyticsNotesEntity>(it) } }.bind()

        val matchupNotes = jsonAccess { jsonObject[TeamlyticsEntity::matchupNotes.name]?.let { json.decodeFromJsonElement<List<MatchupNotesEntity>>(it) } }.bind()

        TeamlyticsEntity(
            id = Uuid.random(),
            name = name,
            pokePaste = pokepaste,
            replays = replays,
            sdNames = sdNames,
            notes = notes,
            lastUpdatedAt = Clock.System.now(),
            matchupNotes = matchupNotes
        )
    }

    private suspend fun loadReplaysFromTeamlyticsReplaysArray(jsonArray: JsonArray): Either<LoadTeamError, List<ReplayAnalyticsEntity>> = either {
        val replayUrlsWithNotes = jsonAccess {
            jsonArray.mapNotNull {
                val url = it.jsonObject[ReplayAnalyticsEntity::url.name]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                url to it.jsonObject[ReplayAnalyticsEntity::notes.name]?.jsonPrimitive?.contentOrNull
            }
        }.bind()

        loadReplays(replayUrlsWithNotes)
    }

    private suspend fun loadReplays(replayUrlsWithNotes: List<Pair<String, String?>>): List<ReplayAnalyticsEntity> {
        val replays = withContext(Dispatchers.Default) {
            replayUrlsWithNotes.map { (url, notes) ->
                async {
                    replayService.fetch(url).getOrElse { error -> null }?.copy(notes = notes)
                }
            }.awaitAll().filterNotNull()
        }
        return replays.map { replayTransformer.toEntity(it) }
    }

    private inline fun <T> jsonAccess(run: () -> T) = Either.catch { run() }
        .mapLeft { LoadTeamError("Invalid save", it) }

    private suspend fun loadFromPokeShowStatsSave(jsonObject: JsonObject): Either<LoadTeamError, TeamlyticsEntity> = either {
        val save = Either.catch {
            json.decodeFromJsonElement<PssTeamlytics>(jsonObject)
        }.mapLeft { LoadTeamError("Invalid PokeShowStats save", it) }
            .bind()
        return Either.Right(save.toTeamlytics())
    }

    private fun decodeJsonObject(byteArray: ByteArray): Either<JsonError, JsonObject> = Either.catch {
        json.parseToJsonElement(byteArray.decodeToString())
    }.mapLeft { error(it) }
        .flatMap {
            if (it is JsonObject) Either.Right(it)
            else Either.Left(JsonError("Not a JSON Object", null))
        }

    private fun error(throwable: Throwable): JsonError =
        JsonError(throwable.message ?: "Invalid JSON", throwable)

    private suspend fun PssTeamlytics.toTeamlytics(): TeamlyticsEntity {
        val pokePaste = pokepaste?.toPokePaste() ?: PokePaste(emptyList())
        val pokepasteString = pokePaste.toPokePasteString()

        val notes = if (teamNotes != null) {
            TeamlyticsNotesEntity(
                teamNotes = teamNotes,
                pokemonNotes = emptyMap()
            )
        } else null

        val replayUrlsWithNotes = replays?.map { it.uri to it.notes } ?: emptyList()
        val replays = loadReplays(replayUrlsWithNotes)
        return TeamlyticsEntity(
            id = Uuid.random(),
            name = saveName,
            pokePaste = pokepasteString,
            replays = replays,
            sdNames = sdNames,
            notes = notes,
            lastUpdatedAt = Clock.System.now()
        )
    }
}

private fun PssPokepaste.toPokePaste(): PokePaste {
    return PokePaste(
        pokemons = pokemons.map { it.toPokemon() }
    )
}

private fun PssPokepastePokemon.toPokemon(): Pokemon {
    return Pokemon(
        name = PokemonName(name),
        surname = null,
        gender = gender?.let { parseGender(it) },
        nature = nature?.let { parseNature(it) },
        item = item,
        shiny = false,
        happiness = 255,
        ability = ability,
        teraType = teraType?.let { parseTeraType(it) },
        level = level ?: 100,
        moves = moves,
        ivs = ivs?.toPokeStats() ?: PokeStats.default(31),
        evs = evs?.toPokeStats() ?: PokeStats.default(0)
    )
}

private fun PssStats.toPokeStats(): PokeStats {
    return PokeStats(
        hp = hp,
        attack = attack,
        defense = defense,
        specialAttack = specialAttack,
        specialDefense = specialDefense,
        speed = speed
    )
}

private fun parseGender(gender: String): Gender? {
    return when (gender.uppercase()) {
        "M", "MALE" -> Gender.MALE
        "F", "FEMALE" -> Gender.FEMALE
        else -> null
    }
}

private fun parseNature(nature: String): Nature? {
    return try {
        Nature.valueOf(nature.uppercase())
    } catch (e: IllegalArgumentException) {
        null
    }
}

private fun parseTeraType(type: String): TeraType? {
    return try {
        TeraType.valueOf(type.uppercase())
    } catch (e: IllegalArgumentException) {
        null
    }
}
