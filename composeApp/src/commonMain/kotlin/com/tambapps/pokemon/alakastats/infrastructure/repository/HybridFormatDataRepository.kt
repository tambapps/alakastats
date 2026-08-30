package com.tambapps.pokemon.alakastats.infrastructure.repository

import arrow.core.Either
import arrow.core.handleErrorWith
import arrow.core.right
import com.tambapps.pokemon.alakastats.domain.error.LoadFormatDataError
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.domain.model.FormatData
import com.tambapps.pokemon.alakastats.domain.repository.FormatDataRepository

// format data is updated more often than the app is released, so we want the remote one when we can
// get it, and the bundled one when we can't (offline, or the file isn't on the repository yet)
class HybridFormatDataRepository(
    private val httpRepository: HttpFormatDataRepository,
    private val localRepository: LocalFormatDataRepository
): FormatDataRepository {

    override suspend fun get(format: Format): Either<LoadFormatDataError, FormatData> {
        return httpRepository.get(format).handleErrorWith { localRepository.get(format) }
    }
}
