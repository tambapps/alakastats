package com.tambapps.pokemon.alakastats.infrastructure.repository

import arrow.core.Either
import arrow.core.raise.either
import com.tambapps.pokemon.alakastats.domain.error.GetTeamlyticsError
import com.tambapps.pokemon.alakastats.domain.error.TeamlyticsNotFound
import com.tambapps.pokemon.alakastats.domain.error.StorageError
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.computeWinRatePercentage
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.alakastats.domain.transformer.toDomain
import com.tambapps.pokemon.alakastats.domain.transformer.toEntity
import com.tambapps.pokemon.alakastats.domain.transformer.toPreview
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.KStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsPreviewEntity
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import kotlin.time.Clock
import kotlin.uuid.Uuid

class KStoreTeamlyticsRepository(
    private val teamsStorage: KStorage<Uuid, TeamlyticsEntity>,
    private val previewsStorage: KStorage<Uuid, TeamlyticsPreviewEntity>,
    private val pokepasteParser: PokepasteParser,
): TeamlyticsRepository {

    override suspend fun listPreviews() = previewsStorage.listEntities().map { it.toDomain() }

    override suspend fun list() = teamsStorage.listEntities().map { it.toDomain(pokepasteParser) }

    override suspend fun get(id: Uuid): Either<GetTeamlyticsError, Teamlytics> = Either.catch {
        teamsStorage.get(id)?.toDomain(pokepasteParser)
            ?: throw NoSuchElementException("Teamlytics with id $id not found")
    }.mapLeft {
        when (it) {
            is NoSuchElementException -> TeamlyticsNotFound(id, it)
            else -> StorageError("Failed to get teamlytics: ${it.message}", it)
        }
    }

    override suspend fun save(teamlytics: Teamlytics): Either<GetTeamlyticsError, Teamlytics> = either {
        val savedTeam = teamsStorage.save(teamlytics.copy(lastUpdatedAt = Clock.System.now()).toEntity())
            .mapLeft { error ->
                StorageError("Failed to save team. No more space left?", error.throwable)
            }.bind()

        val winrate = computeWinRatePercentage(teamlytics.sdNames, teamlytics.replays)
        previewsStorage.save(savedTeam.toPreview(pokepasteParser, winrate))
            .mapLeft { error ->
                StorageError("Failed to save team. No more space left?", error.throwable)
            }
            .bind()

        savedTeam.toDomain(pokepasteParser)
    }

    override suspend fun delete(teamlytics: Teamlytics) {
        val entity = teamlytics.toEntity()
        teamsStorage.delete(entity)
        val winrate = computeWinRatePercentage(teamlytics.sdNames, teamlytics.replays)
        previewsStorage.delete(entity.toPreview(pokepasteParser, winrate))
    }

    override suspend fun delete(id: Uuid) {
        teamsStorage.delete(id)
        previewsStorage.delete(id)
    }
}
