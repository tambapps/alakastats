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
import com.tambapps.pokemon.alakastats.infrastructure.sync.TeamFingerprint
import com.tambapps.pokemon.alakastats.infrastructure.sync.TeamMerger
import com.tambapps.pokemon.alakastats.infrastructure.sync.TeamWriteLock
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import kotlin.time.Clock
import kotlin.uuid.Uuid

class KStoreTeamlyticsRepository(
    private val teamsStorage: KStorage<Uuid, TeamlyticsEntity>,
    private val previewsStorage: KStorage<Uuid, TeamlyticsPreviewEntity>,
    private val pokepasteParser: PokepasteParser,
    private val writeLock: TeamWriteLock,
    private val clock: Clock = Clock.System,
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

    override suspend fun save(teamlytics: Teamlytics, base: Teamlytics?): Either<GetTeamlyticsError, Teamlytics> = writeLock.withLock {
        either {
            val now = clock.now()
            var entity = teamlytics.copy(lastUpdatedAt = now).toEntity()
            val stored = base?.let { teamsStorage.get(teamlytics.id) }
            if (base != null && stored != null && stored.lastUpdatedAt != base.lastUpdatedAt) {
                // the team changed since the version this one comes from (e.g. a sync brought changes): merged,
                // so that what was saved in between isn't lost
                entity = TeamMerger.merge(TeamFingerprint.of(base.toEntity()), entity, stored, localIsNewer = true, now).team
            }
            val savedTeam = teamsStorage.save(entity)
                .mapLeft { error ->
                    StorageError("Failed to save team. No more space left?", error.throwable)
                }.bind()

            val team = savedTeam.toDomain(pokepasteParser)
            val winrate = computeWinRatePercentage(team.sdNames, team.replays)
            previewsStorage.save(savedTeam.toPreview(pokepasteParser, winrate))
                .mapLeft { error ->
                    StorageError("Failed to save team. No more space left?", error.throwable)
                }
                .bind()

            team
        }
    }

    override suspend fun delete(teamlytics: Teamlytics) = delete(teamlytics.id)

    override suspend fun delete(id: Uuid) = writeLock.withLock {
        teamsStorage.delete(id)
        previewsStorage.delete(id)
    }
}
