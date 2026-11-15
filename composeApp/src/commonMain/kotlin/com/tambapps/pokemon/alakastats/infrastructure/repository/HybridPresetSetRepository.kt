package com.tambapps.pokemon.alakastats.infrastructure.repository

import arrow.core.Either
import arrow.core.handleErrorWith
import arrow.core.right
import com.tambapps.pokemon.alakastats.domain.error.LoadPresetSetsError
import com.tambapps.pokemon.alakastats.domain.model.PresetSets
import com.tambapps.pokemon.alakastats.domain.repository.PresetSetRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// the sets are updated more often than the app is released, so we want the remote ones when we can get them, and the
// bundled ones when we can't (offline). Loaded once: they don't change while the app runs
class HybridPresetSetRepository(
    private val httpRepository: HttpPresetSetRepository,
    private val localRepository: LocalPresetSetRepository
): PresetSetRepository {

    private val mutex = Mutex()
    private var sets: PresetSets? = null

    override suspend fun get(): Either<LoadPresetSetsError, PresetSets> = mutex.withLock {
        sets?.let { return it.right() }
        httpRepository.get()
            .handleErrorWith { localRepository.get() }
            .onRight { sets = it }
    }
}
