package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import com.tambapps.pokemon.alakastats.infrastructure.preferences.AppPreferences
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.ShowdownReplaySummaryEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsPreviewEntity
import com.tambapps.pokemon.alakastats.infrastructure.sync.SyncState
import io.github.xxfast.kstore.KStore
import kotlin.uuid.Uuid

expect fun createTeamlyticsKStorage(): KStorage<Uuid, TeamlyticsEntity>

expect fun createTeamlyticsPreviewKStorage(): KStorage<Uuid, TeamlyticsPreviewEntity>

expect fun createSyncStateKStore(): KStore<SyncState>

expect fun createAppPreferencesKStore(): KStore<AppPreferences>

/** For a format, the days having saved Showdown replays, as days since the epoch */
expect fun createShowdownReplayDaysKStore(formatId: String): KStore<List<Long>>

/** The Showdown replays of a format uploaded a day */
expect fun createShowdownReplayDayKStore(formatId: String, day: Long): KStore<List<ShowdownReplaySummaryEntity>>

expect suspend fun downloadToFile(fileName: String, extension: String, bytes: ByteArray): Boolean