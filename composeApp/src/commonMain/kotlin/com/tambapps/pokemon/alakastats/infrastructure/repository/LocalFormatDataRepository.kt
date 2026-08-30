package com.tambapps.pokemon.alakastats.infrastructure.repository

import alakastats.composeapp.generated.resources.Res
import arrow.core.Either
import com.tambapps.pokemon.alakastats.domain.error.LoadFormatDataError
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.domain.model.FormatData
import com.tambapps.pokemon.alakastats.domain.repository.FormatDataRepository
import kotlinx.serialization.json.Json

// reads the format data bundled with the app
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
