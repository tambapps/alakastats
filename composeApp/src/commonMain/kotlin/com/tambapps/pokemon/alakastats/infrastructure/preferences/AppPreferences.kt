package com.tambapps.pokemon.alakastats.infrastructure.preferences

import io.github.xxfast.kstore.KStore
import kotlinx.serialization.Serializable

/**
 * How the user likes the app, on this device only: not synced
 */
@Serializable
data class AppPreferences(
    /** in the replays tab, the games of a same series (e.g. a best-of-3) are grouped */
    val groupReplaysBySeries: Boolean = false,
)

class AppPreferencesRepository(private val store: KStore<AppPreferences>) {

    suspend fun get() = store.get() ?: AppPreferences()

    suspend fun update(transform: (AppPreferences) -> AppPreferences) = store.update { transform(it ?: AppPreferences()) }
}
