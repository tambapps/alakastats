package com.tambapps.pokemon.alakastats.infrastructure.repository

import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.alakastats.domain.transformer.TeamlyticsTransformer
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.KStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import kotlin.uuid.Uuid

class KStoreTeamlyticsRepository(
    private val storage: KStorage<Uuid, TeamlyticsEntity>,
    private val teamlyticsTransformer: TeamlyticsTransformer
): TeamlyticsRepository {

    override suspend fun list() = storage.listEntities().map(teamlyticsTransformer::toDomain)

    override suspend fun save(teamlytics: Teamlytics) = storage.save(teamlyticsTransformer.toEntity(teamlytics))
        .let(teamlyticsTransformer::toDomain)

    override suspend fun delete(teamlytics: Teamlytics) = storage.delete(teamlyticsTransformer.toEntity(teamlytics))
}
