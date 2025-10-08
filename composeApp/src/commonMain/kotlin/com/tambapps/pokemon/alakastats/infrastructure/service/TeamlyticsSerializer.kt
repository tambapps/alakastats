package com.tambapps.pokemon.alakastats.infrastructure.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import com.tambapps.pokemon.alakastats.domain.error.JsonError
import com.tambapps.pokemon.alakastats.domain.error.LoadTeamError
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.transformer.TeamlyticsTransformer
import io.ktor.utils.io.core.toByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class TeamlyticsSerializer(
    private val transformer: TeamlyticsTransformer,
    private val json: Json
) {

    fun export(teamlytics: Teamlytics): ByteArray {
        return json.encodeToString(transformer.toEntity(teamlytics)).toByteArray()
    }


    fun load(byteArray: ByteArray): Either<LoadTeamError, Teamlytics> = either {
        val jsonObject = decode(byteArray).mapLeft { LoadTeamError("Invalid save", it.cause) }
            .bind()
        if (jsonObject["saveName"] != null) loadFromPokeShowStatsSave(jsonObject).bind()
        else load(jsonObject).bind()
    }

    private fun load(jsonObject: JsonObject): Either<LoadTeamError, Teamlytics> = either {
        TODO()
    }


    private fun loadFromPokeShowStatsSave(jsonObject: JsonObject): Either<LoadTeamError, Teamlytics> {
        TODO()
    }

    private fun decode(byteArray: ByteArray): Either<JsonError, JsonObject> = Either.catch {
        json.parseToJsonElement(byteArray.decodeToString())
    }.mapLeft { JsonError(it.message ?: "Invalid JSON", it) }
        .flatMap {
            if (it is JsonObject) Either.Right(it)
            else Either.Left(JsonError("Not a JSON Object", null))
        }
}