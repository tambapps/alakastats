package com.tambapps.pokemon.alakastats.infrastructure.repository

import arrow.core.Either
import arrow.core.raise.either
import com.tambapps.pokemon.alakastats.domain.error.GetTeamlyticsError
import com.tambapps.pokemon.alakastats.domain.error.TeamlyticsNotFound
import com.tambapps.pokemon.alakastats.domain.error.StorageError
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.computeWinRatePercentage
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.alakastats.domain.transformer.TeamlyticsPreviewTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.TeamlyticsTransformer
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.KStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsPreviewEntity
import kotlin.time.Clock
import kotlin.uuid.Uuid

class KStoreTeamlyticsRepository(
    private val teamsStorage: KStorage<Uuid, TeamlyticsEntity>,
    private val previewsStorage: KStorage<Uuid, TeamlyticsPreviewEntity>,
    private val teamlyticsTransformer: TeamlyticsTransformer,
    private val previewTransformer: TeamlyticsPreviewTransformer,
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

    override suspend fun save(teamlytics: Teamlytics): Either<GetTeamlyticsError, Teamlytics> = either {
        val savedTeam = teamsStorage.save(teamlyticsTransformer.toEntity(teamlytics.copy(lastUpdatedAt = Clock.System.now())))
            .mapLeft { error ->
                StorageError("Failed to save team. No more space left?", error.throwable)
            }.bind()

        // save preview
        val winrate = computeWinRatePercentage(teamlytics.sdNames, teamlytics.replays)
        previewsStorage.save(teamlyticsTransformer.toPreview(savedTeam, winrate))
            .mapLeft { error ->
                StorageError("Failed to save team. No more space left?", error.throwable)
            }
            .bind()

        teamlyticsTransformer.toDomain(savedTeam)
    }

    override suspend fun delete(teamlytics: Teamlytics) {
        val entity = teamlyticsTransformer.toEntity(teamlytics)
        teamsStorage.delete(entity)
        val winrate = computeWinRatePercentage(teamlytics.sdNames, teamlytics.replays)
        previewsStorage.delete(teamlyticsTransformer.toPreview(entity, winrate))
    }

    override suspend fun delete(id: Uuid) {
        teamsStorage.delete(id)
        previewsStorage.delete(id)
    }
}
