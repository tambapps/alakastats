package com.tambapps.pokemon.alakastats.domain.repository

import arrow.core.Either
import com.tambapps.pokemon.alakastats.domain.error.LoadPresetSetsError
import com.tambapps.pokemon.alakastats.domain.model.PresetSets

interface PresetSetRepository {

    suspend fun get(): Either<LoadPresetSetsError, PresetSets>
}
