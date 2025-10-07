package com.tambapps.pokemon.alakastats.infrastructure.repository

import arrow.core.Either
import com.tambapps.pokemon.alakastats.domain.error.GetTeamlyticsError
import com.tambapps.pokemon.alakastats.domain.error.TeamlyticsNotFound
import com.tambapps.pokemon.alakastats.domain.error.StorageError
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.alakastats.domain.transformer.TeamlyticsPreviewTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.TeamlyticsTransformer
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.KStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsPreviewEntity
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid

class KStoreTeamlyticsRepository(
    private val teamsStorage: KStorage<Uuid, TeamlyticsEntity>,
    private val previewsStorage: KStorage<Uuid, TeamlyticsPreviewEntity>,
    private val teamlyticsTransformer: TeamlyticsTransformer,
    private val previewTransformer: TeamlyticsPreviewTransformer,
    private val json: Json
): TeamlyticsRepository {

    override suspend fun listPreviews() = previewsStorage.listEntities().map(previewTransformer::toDomain)

    override suspend fun list() = teamsStorage.listEntities().map(teamlyticsTransformer::toDomain)

    override suspend fun get(id: Uuid): Either<GetTeamlyticsError, Teamlytics> = Either.catch {
        teamsStorage.get(id)?.let(teamlyticsTransformer::toDomain)
            ?: throw NoSuchElementException("Teamlytics with id $id not found")
    }.mapLeft { 
        when (it) {
            is NoSuchElementException -> TeamlyticsNotFound(id, it)
            else -> StorageError("Failed to get teamlytics: ${it.message}", it)
        }
    }

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

    override fun exportToJson(teamlytics: Teamlytics): String {
        val entity = teamlyticsTransformer.toEntity(teamlytics)
        return json.encodeToString(entity)
    }
}
