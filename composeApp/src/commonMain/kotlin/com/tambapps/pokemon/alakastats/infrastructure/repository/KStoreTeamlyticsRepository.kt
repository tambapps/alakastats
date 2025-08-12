package com.tambapps.pokemon.alakastats.infrastructure.repository

import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.alakastats.domain.transformer.TeamlyticsPreviewTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.TeamlyticsTransformer
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.KStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsPreviewEntity
import kotlinx.coroutines.coroutineScope
import kotlin.uuid.Uuid

class KStoreTeamlyticsRepository(
    private val teamsStorage: KStorage<Uuid, TeamlyticsEntity>,
    private val previewsStorage: KStorage<Uuid, TeamlyticsPreviewEntity>,
    private val teamlyticsTransformer: TeamlyticsTransformer,
    private val previewTransformer: TeamlyticsPreviewTransformer,
): TeamlyticsRepository {

    override suspend fun listPreviews() = previewsStorage.listEntities().map(previewTransformer::toDomain)

    override suspend fun list() = teamsStorage.listEntities().map(teamlyticsTransformer::toDomain)

    override suspend fun get(id: Uuid) = teamsStorage.get(id)?.let(teamlyticsTransformer::toDomain)

    override suspend fun save(teamlytics: Teamlytics): Teamlytics = coroutineScope {
        val savedTeam = teamsStorage.save(teamlyticsTransformer.toEntity(teamlytics))
        previewsStorage.save(teamlyticsTransformer.toPreview(savedTeam))
        teamlyticsTransformer.toDomain(savedTeam)
    }

    override suspend fun delete(teamlytics: Teamlytics) {
        val entity = teamlyticsTransformer.toEntity(teamlytics)
        teamsStorage.delete(entity)
        previewsStorage.delete(teamlyticsTransformer.toPreview(entity))
    }

    override suspend fun delete(id: Uuid) {
        teamsStorage.delete(id)
        previewsStorage.delete(id)
    }
}
