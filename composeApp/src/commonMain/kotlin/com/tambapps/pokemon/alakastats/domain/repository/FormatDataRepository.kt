package com.tambapps.pokemon.alakastats.domain.repository

import arrow.core.Either
import com.tambapps.pokemon.alakastats.domain.error.LoadFormatDataError
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.domain.model.FormatData

interface FormatDataRepository {

    suspend fun get(format: Format): Either<LoadFormatDataError, FormatData>


}