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
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.PssTeamlytics
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.ReplayAnalyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsNotesEntity
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
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

    suspend fun load(byteArray: ByteArray): Either<LoadTeamError, Teamlytics> = either {
        val jsonObject = decode(byteArray).mapLeft { LoadTeamError("Invalid save", it.cause) }
            .bind()
        val entity = if (jsonObject["saveName"] != null) loadFromPokeShowStatsSave(jsonObject).bind()
        else load(jsonObject).bind()
        transformer.toDomain(entity)
    }

    private suspend fun load(jsonObject: JsonObject): Either<LoadTeamError, TeamlyticsEntity> = either {
        val name = jsonAccess { jsonObject[TeamlyticsEntity::name.name]?.jsonPrimitive?.contentOrNull ?: "<no name>" }.bind()
        val pokepaste = jsonAccess { jsonObject[TeamlyticsEntity::pokePaste.name]?.jsonPrimitive?.contentOrNull }.bind()
            ?: ""
        val replays = jsonAccess { jsonObject[TeamlyticsEntity::replays.name]?.jsonArray }
            .flatMap { if (it != null) loadReplays(it) else Either.Right(listOf()) }
            .bind()
        val sdNames = jsonAccess { jsonObject[TeamlyticsEntity::sdNames.name]?.jsonArray?.map { it.jsonPrimitive.content } }.bind()
            ?: listOf()
        val notes = jsonAccess { jsonObject["notes"]?.let { json.decodeFromJsonElement<TeamlyticsNotesEntity>(it) } }.bind()

        TeamlyticsEntity(
            id = Uuid.random(),
            name = name,
            pokePaste = pokepaste,
            replays = replays,
            sdNames = sdNames,
            notes = notes,
            lastUpdatedAt = Clock.System.now()
        )
    }

    private suspend fun loadReplays(jsonArray: JsonArray): Either<LoadTeamError, List<ReplayAnalyticsEntity>> = either {
        val replayUrlsWithNotes = jsonAccess {
            jsonArray.mapNotNull {
                val url = it.jsonObject[ReplayAnalyticsEntity::url.name]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                url to it.jsonObject[ReplayAnalyticsEntity::notes.name]?.jsonPrimitive?.contentOrNull
            }
        }.bind()

        val replays = withContext(Dispatchers.Default) {
            replayUrlsWithNotes.map { (url, notes) ->
                async {
                    replayService.fetch(url).getOrElse { error -> null }?.copy(notes = notes)
                }
            }.awaitAll().filterNotNull()
        }
        replays.map { replayTransformer.toEntity(it) }
    }

    private inline fun <T> jsonAccess(run: () -> T) = Either.catch { run() }
        .mapLeft { LoadTeamError("Invalid save", it) }

    private fun loadFromPokeShowStatsSave(jsonObject: JsonObject): Either<LoadTeamError, TeamlyticsEntity> = either {
        val save = Either.catch {
            json.decodeFromJsonElement<PssTeamlytics>(jsonObject)
        }.mapLeft { LoadTeamError("Invalid PokeShowStats save", it) }
            .bind()
        return Either.Right(save.toTeamlytics())
    }

    private fun decode(byteArray: ByteArray): Either<JsonError, JsonObject> = Either.catch {
        json.parseToJsonElement(byteArray.decodeToString())
    }.mapLeft { error(it) }
        .flatMap {
            if (it is JsonObject) Either.Right(it)
            else Either.Left(JsonError("Not a JSON Object", null))
        }

    private fun error(throwable: Throwable): JsonError =
        JsonError(throwable.message ?: "Invalid JSON", throwable)
}

fun PssTeamlytics.toTeamlytics(): TeamlyticsEntity = TODO()
