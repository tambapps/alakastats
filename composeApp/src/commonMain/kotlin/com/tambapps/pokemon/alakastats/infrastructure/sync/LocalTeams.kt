package com.tambapps.pokemon.alakastats.infrastructure.sync

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.tambapps.pokemon.alakastats.domain.model.computeWinRatePercentage
import com.tambapps.pokemon.alakastats.domain.transformer.toDomain
import com.tambapps.pokemon.alakastats.domain.transformer.toPreview
import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudError
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.KStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.KStorageError
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsPreviewEntity
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * The teams of this device, as the sync reads and writes them: the stored team, and its preview regenerated
 * from it. Writes take the [TeamWriteLock] shared with the app's saves.
 */
class LocalTeams(
    private val teamsStorage: KStorage<Uuid, TeamlyticsEntity>,
    private val previewsStorage: KStorage<Uuid, TeamlyticsPreviewEntity>,
    private val pokepasteParser: PokepasteParser,
    private val writeLock: TeamWriteLock,
) {

    suspend fun get(id: Uuid) = teamsStorage.get(id)

    suspend fun name(id: Uuid) = previewsStorage.get(id)?.name

    suspend fun versions(): List<LocalTeamVersion> {
        val previews = previewsStorage.listEntities().associateBy { it.id }
        return teamsStorage.listIds().map { id ->
            // the preview holds the same date and flag as the team, and is way lighter to load
            val preview = previews[id]
            if (preview != null) LocalTeamVersion(id, preview.lastUpdatedAt, preview.localOnly)
            else teamsStorage.get(id).let { LocalTeamVersion(id, it?.lastUpdatedAt, it?.localOnly == true) }
        }
    }

    /** Whether the team is kept on this device only, out of the sync */
    suspend fun isLocalOnly(id: Uuid) = previewsStorage.get(id)?.localOnly ?: (teamsStorage.get(id)?.localOnly == true)

    suspend fun save(entity: TeamlyticsEntity) = writeLock.withLock { saveUnlocked(entity) }

    suspend fun delete(id: Uuid) = writeLock.withLock {
        teamsStorage.delete(id)
        previewsStorage.delete(id)
    }

    /**
     * Takes a version from the cloud: as is if the local team didn't change, merged with it otherwise. Under the
     * lock, so that a save of the app in the meantime is merged instead of overwritten
     * @param localChanged whether the local team changed since the version [base] describes
     * @return the merge, null if the remote version was taken as is
     */
    suspend fun applyRemote(
        remote: TeamlyticsEntity,
        base: TeamFingerprint?,
        now: Instant,
        localChanged: (TeamlyticsEntity) -> Boolean,
    ): Either<CloudError, MergeResult?> = writeLock.withLock {
        val local = teamsStorage.get(remote.id)
        if (local == null || !localChanged(local)) {
            // saved as is: the lastUpdatedAt of the remote version is kept
            saveUnlocked(remote).getOrElse { return@withLock it.left() }
            null.right()
        } else {
            val localIsNewer = (local.lastUpdatedAt ?: Instant.DISTANT_PAST) >= (remote.lastUpdatedAt ?: Instant.DISTANT_PAST)
            val result = TeamMerger.merge(base, local, remote, localIsNewer, now)
            saveUnlocked(result.team).getOrElse { return@withLock it.left() }
            result.right()
        }
    }

    private suspend fun saveUnlocked(entity: TeamlyticsEntity): Either<CloudError, Unit> {
        teamsStorage.save(entity).getOrElse { return localSaveError(it).left() }
        val team = entity.toDomain(pokepasteParser)
        previewsStorage.save(entity.toPreview(pokepasteParser, computeWinRatePercentage(team.sdNames, team.replays)))
            .getOrElse { return localSaveError(it).left() }
        return Unit.right()
    }

    // e.g. the browser's storage is full
    private fun localSaveError(error: KStorageError) =
        CloudError.Unknown("Couldn't save it on this device, maybe no more space left (${error.throwable?.message ?: error.message})", error.throwable)
}

/** compared at the millisecond: the cloud only stores milliseconds */
internal fun Instant?.sameMillisAs(other: Instant?) = this?.toEpochMilliseconds() == other?.toEpochMilliseconds()
