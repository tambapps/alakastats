package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsPreviewEntity
import com.tambapps.pokemon.alakastats.infrastructure.sync.SyncState
import io.github.xxfast.kstore.KStore
import kotlin.uuid.Uuid

expect fun createTeamlyticsKStorage(): KStorage<Uuid, TeamlyticsEntity>

expect fun createTeamlyticsPreviewKStorage(): KStorage<Uuid, TeamlyticsPreviewEntity>

expect fun createSyncStateKStore(): KStore<SyncState>

expect suspend fun downloadToFile(fileName: String, extension: String, bytes: ByteArray): Boolean