package com.tambapps.pokemon.alakastats.infrastructure.sync

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.tambapps.pokemon.alakastats.domain.model.computeWinRatePercentage
import com.tambapps.pokemon.alakastats.domain.transformer.toDomain
import com.tambapps.pokemon.alakastats.domain.transformer.toPreview
import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudError
import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudFile
import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudStorage
import com.tambapps.pokemon.alakastats.infrastructure.cloud.auth.OAuthTokenProvider
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.KStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.KStorageError
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsPreviewEntity
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.Uuid

sealed interface SyncStatus {
    /** No cloud provider on this platform */
    data object Unavailable : SyncStatus
    data object Disconnected : SyncStatus
    /** @param failures the teams that couldn't be synced. They're retried at the next sync */
    data class Idle(val lastSyncAt: Instant?, val failures: List<SyncFailure> = emptyList()) : SyncStatus
    data class Syncing(val lastSyncAt: Instant?) : SyncStatus
    data class Failed(val error: CloudError, val lastSyncAt: Instant?) : SyncStatus {
        /** The user has to connect again for the sync to work */
        val needsReconnection get() = error is CloudError.AccessRevoked || error is CloudError.NeedsUserInteraction
    }
}

/**
 * @param teamName null when the team isn't on this device (e.g. a cloud file that couldn't be downloaded)
 */
data class SyncFailure(val teamId: Uuid, val teamName: String?, val reason: String)

/**
 * Synchronises the local teams with a [CloudStorage]. Optional: nothing happens until the user connects.
 */
class TeamSyncService(
    private val cloudStorage: CloudStorage?,
    private val tokenProvider: OAuthTokenProvider?,
    private val teamsStorage: KStorage<Uuid, TeamlyticsEntity>,
    private val previewsStorage: KStorage<Uuid, TeamlyticsPreviewEntity>,
    private val stateStore: SyncStateStore,
    private val pokepasteParser: PokepasteParser,
    private val json: Json,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val clock: Clock = Clock.System,
    private val debounce: Duration = 3.seconds,
) {

    val isAvailable = cloudStorage != null && tokenProvider != null

    private val _status = MutableStateFlow(if (isAvailable) SyncStatus.Disconnected else SyncStatus.Unavailable)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val _teamsChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emits when a sync changed the local teams */
    val teamsChanged: SharedFlow<Unit> = _teamsChanged.asSharedFlow()

    private val syncMutex = Mutex()
    private val stateMutex = Mutex()
    private var state: SyncState? = null
    private var debounceJob: Job? = null

    /**
     * Loads the sync state and syncs if connected. To call once, at launch
     */
    suspend fun start() {
        if (!isAvailable) return
        val state = currentState()
        _status.value = if (state.connected) SyncStatus.Idle(state.lastSyncAt) else SyncStatus.Disconnected
        if (state.connected) sync()
    }

    suspend fun connect(): Either<CloudError, Unit> {
        val tokenProvider = tokenProvider ?: return CloudError.Unknown("Cloud sync isn't available").left()
        tokenProvider.accessToken(interactive = true).getOrElse { return it.left() }
        updateState { it.copy(connected = true) }
        val status = sync()
        return if (status is SyncStatus.Failed) status.error.left() else Unit.right()
    }

    /**
     * Stops syncing. The local teams and the cloud files are both kept
     */
    suspend fun disconnect() {
        debounceJob?.cancel()
        syncMutex.withLock {
            tokenProvider?.signOut()
            updateState { SyncState() }
            _status.value = if (isAvailable) SyncStatus.Disconnected else SyncStatus.Unavailable
        }
    }

    /**
     * Syncs a bit later, so that a burst of local changes gives a single sync
     */
    fun requestSync() {
        if (!isAvailable) return
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(debounce)
            // launched apart from the debounce job, so that a new request doesn't cancel a running sync
            scope.launch { sync() }
        }
    }

    suspend fun onLocalDelete(id: Uuid) {
        if (!isAvailable || !currentState().connected) return
        val now = clock.now()
        updateState { it.copy(tombstones = it.tombstones + (id to now), entries = it.entries - id) }
        requestSync()
    }

    suspend fun sync(): SyncStatus = syncMutex.withLock {
        val cloudStorage = cloudStorage ?: return SyncStatus.Unavailable
        val initialState = currentState()
        if (!initialState.connected) return SyncStatus.Disconnected.also { _status.value = it }
        _status.value = SyncStatus.Syncing(initialState.lastSyncAt)

        val status = try {
            doSync(cloudStorage)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SyncStatus.Failed(CloudError.Unknown(e.message ?: "Sync failed", e), currentState().lastSyncAt)
        }
        _status.value = status
        status
    }

    private suspend fun doSync(cloudStorage: CloudStorage): SyncStatus {
        val remoteFiles = cloudStorage.list().getOrElse { return failed(it) }
        val remote = remoteFiles.mapNotNull { it.toRemoteTeamVersion() }
        val local = localVersions()
        val actions = SyncPlanner.plan(local, remote, currentState())

        val failures = mutableListOf<SyncFailure>()
        var teamsChanged = false
        for (action in actions) {
            val result = when (action) {
                is SyncAction.Upload -> upload(cloudStorage, action)
                is SyncAction.Download -> download(cloudStorage, action).onRight { teamsChanged = true }
                is SyncAction.DeleteLocal -> deleteLocal(action.teamId).onRight { teamsChanged = true }
                is SyncAction.DeleteRemote -> deleteRemote(cloudStorage, action)
                is SyncAction.MarkSynced -> updateState {
                    it.copy(entries = it.entries + (action.teamId to SyncedEntry(action.fileId, action.updatedAt)))
                }.right()
            }
            val error = result.leftOrNull() ?: continue
            if (error.stopsSync()) {
                if (teamsChanged) _teamsChanged.tryEmit(Unit)
                return failed(error)
            }
            val failure = SyncFailure(action.teamId, previewsStorage.get(action.teamId)?.name, error.message)
            println("Couldn't sync team ${failure.teamId} (${failure.teamName}): ${failure.reason}")
            failures.add(failure)
        }

        // tombstones of teams no longer in the cloud are useless
        val remoteIds = remote.map { it.id }.toSet()
        val now = clock.now()
        val state = updateState { s -> s.copy(lastSyncAt = now, tombstones = s.tombstones.filterKeys { it in remoteIds }) }
        if (teamsChanged) _teamsChanged.tryEmit(Unit)
        return SyncStatus.Idle(state.lastSyncAt, failures)
    }

    private suspend fun localVersions(): List<LocalTeamVersion> {
        val previews = previewsStorage.listEntities().associateBy { it.id }
        return teamsStorage.listIds().map { id ->
            // the preview holds the same date as the team, and is way lighter to load
            val updatedAt = previews[id]?.lastUpdatedAt ?: teamsStorage.get(id)?.lastUpdatedAt
            LocalTeamVersion(id, updatedAt)
        }
    }

    private suspend fun upload(cloudStorage: CloudStorage, action: SyncAction.Upload): Either<CloudError, Unit> {
        val entity = teamsStorage.get(action.teamId) ?: return Unit.right()
        val bytes = json.encodeToString(entity).encodeToByteArray()
        val properties = remoteProperties(entity)
        var uploaded = cloudStorage.upload(fileName(entity.id), bytes, properties, action.existingFileId)
        if (action.existingFileId != null && uploaded.leftOrNull() is CloudError.NotFound) {
            // deleted in the meantime: create it again
            uploaded = cloudStorage.upload(fileName(entity.id), bytes, properties, existingFileId = null)
        }
        val file = uploaded.getOrElse { return it.left() }
        updateState { it.copy(entries = it.entries + (entity.id to SyncedEntry(file.id, entity.lastUpdatedAt))) }
        return Unit.right()
    }

    private suspend fun download(cloudStorage: CloudStorage, action: SyncAction.Download): Either<CloudError, Unit> {
        val bytes = cloudStorage.download(action.fileId).getOrElse { return it.left() }
        val entity = try {
            json.decodeFromString<TeamlyticsEntity>(bytes.decodeToString())
        } catch (e: Exception) {
            return CloudError.Unknown("The Google Drive copy can't be read (${e.message})", e).left()
        }
        if (entity.id != action.teamId) return CloudError.Unknown("The Google Drive copy belongs to another team").left()

        // saved as is: the lastUpdatedAt of the remote version is kept
        teamsStorage.save(entity).getOrElse { return localSaveError(it).left() }
        val team = entity.toDomain(pokepasteParser)
        previewsStorage.save(entity.toPreview(pokepasteParser, computeWinRatePercentage(team.sdNames, team.replays)))
            .getOrElse { return localSaveError(it).left() }
        updateState { it.copy(entries = it.entries + (entity.id to SyncedEntry(action.fileId, entity.lastUpdatedAt))) }
        return Unit.right()
    }

    private suspend fun deleteLocal(id: Uuid): Either<CloudError, Unit> {
        teamsStorage.delete(id)
        previewsStorage.delete(id)
        updateState { it.copy(entries = it.entries - id) }
        return Unit.right()
    }

    private suspend fun deleteRemote(cloudStorage: CloudStorage, action: SyncAction.DeleteRemote): Either<CloudError, Unit> {
        val deleted = cloudStorage.delete(action.fileId)
        if (deleted.isLeft() && deleted.leftOrNull() !is CloudError.NotFound) return deleted
        updateState { state ->
            val entry = state.entries[action.teamId]
            // a duplicate file of a team still synced keeps its entry
            if (entry != null && entry.remoteFileId != action.fileId) state
            else state.copy(entries = state.entries - action.teamId, tombstones = state.tombstones - action.teamId)
        }
        return Unit.right()
    }

    // e.g. the browser's storage is full
    private fun localSaveError(error: KStorageError) =
        CloudError.Unknown("Couldn't save it on this device, maybe no more space left (${error.throwable?.message ?: error.message})", error.throwable)

    private suspend fun failed(error: CloudError) = SyncStatus.Failed(error, currentState().lastSyncAt)

    private fun CloudError.stopsSync() = when (this) {
        is CloudError.NotFound, is CloudError.Unknown -> false
        else -> true
    }

    private suspend fun currentState(): SyncState = stateMutex.withLock {
        state ?: stateStore.load().also { state = it }
    }

    private suspend fun updateState(transform: (SyncState) -> SyncState): SyncState = stateMutex.withLock {
        val newState = transform(state ?: stateStore.load())
        stateStore.save(newState)
        state = newState
        newState
    }

    private fun CloudFile.toRemoteTeamVersion(): RemoteTeamVersion? {
        val schema = properties[PROPERTY_SCHEMA]?.toIntOrNull() ?: return null
        // saved by a newer version of the app: can't be read
        if (schema > SCHEMA_VERSION) return null
        val id = properties[PROPERTY_TEAM_ID]?.let { Uuid.parseOrNull(it) } ?: return null
        val updatedAt = properties[PROPERTY_UPDATED_AT]?.toLongOrNull()?.let { Instant.fromEpochMilliseconds(it) }
        return RemoteTeamVersion(id, this.id, updatedAt)
    }

    companion object {
        const val SCHEMA_VERSION = 1
        const val PROPERTY_TEAM_ID = "teamId"
        const val PROPERTY_UPDATED_AT = "updatedAt"
        const val PROPERTY_SCHEMA = "schema"

        fun fileName(id: Uuid) = "team-$id.json"

        fun remoteProperties(entity: TeamlyticsEntity) = buildMap {
            put(PROPERTY_TEAM_ID, entity.id.toString())
            put(PROPERTY_SCHEMA, SCHEMA_VERSION.toString())
            entity.lastUpdatedAt?.let { put(PROPERTY_UPDATED_AT, it.toEpochMilliseconds().toString()) }
        }
    }
}
