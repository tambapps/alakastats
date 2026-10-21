package com.tambapps.pokemon.alakastats.infrastructure.sync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Taken to write a team locally, by the app's saves and by the sync, so that one can't overwrite what the other
 * just wrote between its read and its write. Not reentrant.
 */
class TeamWriteLock {
    private val mutex = Mutex()

    suspend fun <T> withLock(block: suspend () -> T): T = mutex.withLock { block() }
}
