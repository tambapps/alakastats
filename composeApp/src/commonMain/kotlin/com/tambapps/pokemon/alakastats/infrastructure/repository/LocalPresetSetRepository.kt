package com.tambapps.pokemon.alakastats.infrastructure.repository

import alakastats.composeapp.generated.resources.Res
import arrow.core.Either
import com.tambapps.pokemon.alakastats.domain.error.LoadPresetSetsError
import com.tambapps.pokemon.alakastats.domain.model.PresetSets
import com.tambapps.pokemon.alakastats.domain.repository.PresetSetRepository
import kotlinx.serialization.json.Json

internal const val PRESET_SETS_FILE = "files/sets/champions.json"

// reads the sets bundled with the app
class LocalPresetSetRepository(
    private val json: Json
): PresetSetRepository {

    override suspend fun get(): Either<LoadPresetSetsError, PresetSets> {
        return Either.catch { Res.readBytes(PRESET_SETS_FILE).decodeToString() }
            .map { json.decodeFromString<PresetSetsEntity>(it).toDomain() }
            .mapLeft { LoadPresetSetsError("Couldn't load the sets: ${it.message}", it) }
    }
}
