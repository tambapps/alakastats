package com.tambapps.pokemon.alakastats.infrastructure.repository

import alakastats.composeapp.generated.resources.Res
import arrow.core.Either
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.error.LoadFormatDataError
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.domain.model.FormatData
import com.tambapps.pokemon.alakastats.domain.repository.FormatDataRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FormatDataEntity(
    val popularPokemons: List<String>
)
class LocalFormatDataRepository(
    private val json: Json
): FormatDataRepository {

    override suspend fun get(format: Format): Either<LoadFormatDataError, FormatData> {
        return Either.catch { Res.readBytes("files/formats/${format.name}.json").decodeToString() }
            .map { json.decodeFromString<FormatDataEntity>(it) }
            .map { it.toDomain() }
            .mapLeft { LoadFormatDataError("Couldn't load format data: ${it.message}") }
    }
}

private fun FormatDataEntity.toDomain() = FormatData(
    popularPokemons = popularPokemons.map { PokemonName(it) }
)