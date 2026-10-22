package com.tambapps.pokemon.alakastats.infrastructure.sync

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudError
import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudFile
import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudStorage
import com.tambapps.pokemon.alakastats.infrastructure.cloud.Member
import com.tambapps.pokemon.alakastats.infrastructure.cloud.ShareRole
import com.tambapps.pokemon.alakastats.infrastructure.cloud.SharingCloudStorage
import com.tambapps.pokemon.alakastats.infrastructure.cloud.auth.OAuthTokenProvider
import com.tambapps.pokemon.alakastats.infrastructure.cloud.auth.SHARING_SCOPES
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.KStorage
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
    /**
     * @param failures the teams that couldn't be synced. They're retried at the next sync
     * @param merges the teams changed on both sides, merged by the last sync
     */
    data class Idle(
        val lastSyncAt: Instant?,
        val failures: List<SyncFailure> = emptyList(),
        val merges: List<TeamMerge> = emptyList(),
        /** the shared teams can't be synced on this device until the user allows it */
        val sharingNeedsConsent: Boolean = false,
    ) : SyncStatus
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
 * A team changed both on this device and in the cloud, merged by a sync
 */
data class TeamMerge(val teamId: Uuid, val teamName: String, val summary: MergeSummary)

/**
 * Where a local team stands with the cloud
 */
enum class TeamCloudState {
    /** the cloud has this version */
    SYNCED,
    /** changed since its last sync, or never synced: sent at the next sync */
    PENDING,
    /** the last sync couldn't sync it */
    FAILED,
    /** synced before the cloud was disconnected: its copy there isn't kept up to date anymore */
    DISCONNECTED,
}

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
    private val writeLock: TeamWriteLock,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val clock: Clock = Clock.System,
    private val debounce: Duration = 3.seconds,
) {

    val isAvailable = cloudStorage != null && tokenProvider != null

    private val _status = MutableStateFlow(if (isAvailable) SyncStatus.Disconnected else SyncStatus.Unavailable)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val _teamsChanged = MutableSharedFlow<Set<Uuid>>(extraBufferCapacity = 8)
    /** Emits the ids of the local teams a sync changed */
    val teamsChanged: SharedFlow<Set<Uuid>> = _teamsChanged.asSharedFlow()

    private val _merges = MutableSharedFlow<TeamMerge>(extraBufferCapacity = 16)
    /** Emits each team merged by a sync, to tell the user */
    val merges: SharedFlow<TeamMerge> = _merges.asSharedFlow()

    private val _notices = MutableSharedFlow<String>(extraBufferCapacity = 16)
    /** Emits what a sync has to tell the user, e.g. a shared team no longer shared */
    val notices: SharedFlow<String> = _notices.asSharedFlow()

    private val _sharing = MutableStateFlow<Map<Uuid, ShareRole>>(emptyMap())
    /** The role of the user in each shared team of this device */
    val sharing: StateFlow<Map<Uuid, ShareRole>> = _sharing.asStateFlow()

    private val _formerSharing = MutableStateFlow<Map<Uuid, ShareRole>>(emptyMap())
    /** Once disconnected, the user's role in the teams that were shared: still shared, but not synced anymore */
    val formerSharing: StateFlow<Map<Uuid, ShareRole>> = _formerSharing.asStateFlow()

    private val syncMutex = Mutex()
    private val stateMutex = Mutex()
    private var state: SyncState? = null
    private var debounceJob: Job? = null

    private val localTeams = LocalTeams(teamsStorage, previewsStorage, pokepasteParser, writeLock)
    private val stateAccess = object : SyncStateAccess {
        override suspend fun current() = currentState()
        override suspend fun update(transform: (SyncState) -> SyncState) = updateState(transform)
    }
    private val sharedSync = (cloudStorage as? SharingCloudStorage)?.let { SharedTeamSync(it, localTeams, stateAccess, json, clock) }

    /** Whether teams can be shared with other users on this platform */
    val isSharingAvailable = isAvailable && sharedSync != null

    /** A viewer of a shared team can't change it */
    fun isReadOnly(teamId: Uuid) = _sharing.value[teamId] == ShareRole.VIEWER

    private val _teamStates = MutableStateFlow<Map<Uuid, TeamCloudState>>(emptyMap())
    /** Where each local team stands with the cloud. Empty when the cloud isn't connected */
    val teamStates: StateFlow<Map<Uuid, TeamCloudState>> = _teamStates.asStateFlow()
    private var lastFailedTeams = emptySet<Uuid>()

    private suspend fun refreshTeamStates() {
        val state = currentState()
        if (!isAvailable) {
            _teamStates.value = emptyMap()
            return
        }
        if (!state.connected) {
            val localIds = localTeams.versions().map { it.id }.toSet()
            _teamStates.value = (state.formerlyLinkedTeams intersect localIds).associateWith { TeamCloudState.DISCONNECTED }
            return
        }
        _teamStates.value = localTeams.versions().associate { version ->
            val syncedAt = state.sharedEntries[version.id]?.synced?.syncedUpdatedAt ?: state.entries[version.id]?.syncedUpdatedAt
            version.id to when {
                version.id in lastFailedTeams -> TeamCloudState.FAILED
                syncedAt == null || !version.updatedAt.sameMillisAs(syncedAt) -> TeamCloudState.PENDING
                else -> TeamCloudState.SYNCED
            }
        }
    }

    /**
     * Loads the sync state and syncs if connected. To call once, at launch
     */
    suspend fun start() {
        if (!isAvailable) return
        val state = currentState()
        _status.value = if (state.connected) SyncStatus.Idle(state.lastSyncAt) else SyncStatus.Disconnected
        refreshTeamStates()
        if (state.connected) sync()
    }

    suspend fun connect(): Either<CloudError, Unit> {
        val tokenProvider = tokenProvider ?: return CloudError.Unknown("Cloud sync isn't available").left()
        tokenProvider.accessToken(interactive = true).getOrElse { return it.left() }
        // the sync takes over from what was known at the disconnection
        updateState { it.copy(connected = true, formerlyLinkedTeams = emptySet(), formerlySharedRoles = emptyMap()) }
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
            // everything else is forgotten, so that a next connection (maybe to another account) starts clean
            updateState { state ->
                SyncState(
                    formerlyLinkedTeams = state.entries.keys + state.sharedEntries.keys + state.formerlyLinkedTeams,
                    formerlySharedRoles = state.formerlySharedRoles + state.sharedEntries.mapValues { (_, entry) -> entry.role },
                )
            }
            lastFailedTeams = emptySet()
            _status.value = if (isAvailable) SyncStatus.Disconnected else SyncStatus.Unavailable
        }
        refreshTeamStates()
    }

    /**
     * Syncs a bit later, so that a burst of local changes gives a single sync
     */
    fun requestSync() {
        if (!isAvailable) return
        // the change shows as pending right away
        scope.launch { refreshTeamStates() }
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(debounce)
            // launched apart from the debounce job, so that a new request doesn't cancel a running sync
            scope.launch { sync() }
        }
    }

    /**
     * Syncs right away, unless a sync is running or happened recently. For when the user may find changes made
     * elsewhere: the app back to the foreground, a team opened
     */
    fun syncIfStale(maxAge: Duration = 30.seconds) {
        if (!isAvailable || _status.value is SyncStatus.Syncing) return
        scope.launch {
            val lastSyncAt = currentState().lastSyncAt
            if (lastSyncAt == null || clock.now() - lastSyncAt > maxAge) sync()
        }
    }

    /**
     * A team was deleted on this device. A shared team is deleted for everyone by its owner, and left by the others
     */
    suspend fun onLocalDelete(id: Uuid) {
        if (!isAvailable) return
        if (!currentState().connected) {
            if (id in currentState().formerlyLinkedTeams) {
                updateState { it.copy(formerlyLinkedTeams = it.formerlyLinkedTeams - id, formerlySharedRoles = it.formerlySharedRoles - id) }
                refreshTeamStates()
            }
            return
        }
        val role = currentState().sharedEntries[id]?.role
        if (role != null && sharedSync != null) {
            syncMutex.withLock {
                if (role == ShareRole.OWNER) sharedSync.deleteForEveryone(id) else sharedSync.leave(id)
            }
            requestSync()
            return
        }
        val now = clock.now()
        updateState { it.copy(tombstones = it.tombstones + (id to now), entries = it.entries - id) }
        requestSync()
    }

    /**
     * Asks the user the access to the shared files (picked or created by the app), if not given yet
     */
    suspend fun authorizeSharing(): Either<CloudError, Unit> = sharingAccessToken().map { }

    /**
     * A token reaching the shared files, e.g. for the file picker. Asks the user's consent if needed
     */
    suspend fun sharingAccessToken(): Either<CloudError, String> {
        val tokenProvider = tokenProvider ?: return CloudError.Unknown("Cloud sync isn't available").left()
        return tokenProvider.accessToken(interactive = true, SHARING_SCOPES)
    }

    /**
     * Shares the team with someone. The first time, the team moves from the private space to a file of the user's
     * cloud storage
     */
    suspend fun shareTeam(teamId: Uuid, email: String, role: ShareRole): Either<CloudError, Unit> {
        val sharedSync = sharedSync ?: return CloudError.Unknown("Sharing isn't available").left()
        if (!currentState().connected) return CloudError.Unknown("Connect your Google Drive in the Sync screen first").left()
        authorizeSharing().getOrElse { return it.left() }
        syncMutex.withLock {
            val privateFileId = currentState().entries[teamId]?.remoteFileId
            sharedSync.makeShared(teamId, privateFileId).getOrElse { return it.left() }
            sharedSync.share(teamId, email, role).getOrElse { return it.left() }
        }
        // the other devices learn about it through the index
        sync()
        return Unit.right()
    }

    suspend fun members(teamId: Uuid): Either<CloudError, List<Member>> =
        sharedSync?.members(teamId) ?: emptyList<Member>().right()

    suspend fun setMemberRole(teamId: Uuid, permissionId: String, role: ShareRole): Either<CloudError, Unit> =
        sharedSync?.setMemberRole(teamId, permissionId, role) ?: Unit.right()

    suspend fun removeMember(teamId: Uuid, permissionId: String): Either<CloudError, Unit> =
        sharedSync?.removeMember(teamId, permissionId) ?: Unit.right()

    /**
     * The owner stops sharing the team: it becomes private again, the members keep a private copy
     */
    suspend fun stopSharing(teamId: Uuid): Either<CloudError, Unit> {
        val sharedSync = sharedSync ?: return Unit.right()
        syncMutex.withLock { sharedSync.stopSharing(teamId) }
        sync()
        return Unit.right()
    }

    /**
     * Joins teams shared with the user, whose files the user picked
     * @return the names of the teams joined
     */
    suspend fun joinSharedTeams(fileIds: List<String>): Either<CloudError, List<String>> {
        val sharedSync = sharedSync ?: return CloudError.Unknown("Sharing isn't available").left()
        val joined = syncMutex.withLock { sharedSync.join(fileIds) }.getOrElse { return it.left() }
        _teamsChanged.tryEmit(currentState().sharedEntries.keys)
        sync()
        return joined.right()
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
        // a sync stopped early (e.g. offline) says nothing about the teams it didn't reach
        if (status is SyncStatus.Idle) lastFailedTeams = status.failures.map { it.teamId }.toSet()
        refreshTeamStates()
        status
    }

    private suspend fun doSync(cloudStorage: CloudStorage): SyncStatus {
        val remoteFiles = cloudStorage.list().getOrElse { return failed(it) }
        val report = SyncReport()

        // the shared teams are out of the private space
        val index = sharedSync?.let { shared ->
            val index = shared.syncIndex(remoteFiles).getOrElse { return failed(it) }
            report.changedTeams.addAll(shared.applyRemovals(index))
            index
        }
        val sharedIds = index?.sharedTeamIds.orEmpty()

        val remote = remoteFiles.mapNotNull { it.toRemoteTeamVersion() }
        val (sharedLeftovers, privateRemote) = remote.partition { it.id in sharedIds }
        val local = localTeams.versions().filter { it.id !in sharedIds }
        val state = updateState { it.copy(entries = it.entries - sharedIds, tombstones = it.tombstones - sharedIds) }
        // a private copy left by a team that moved to the shared space
        val actions = sharedLeftovers.map { SyncAction.DeleteRemote(it.id, it.fileId) } + SyncPlanner.plan(local, privateRemote, state)

        for (action in actions) {
            val result = when (action) {
                is SyncAction.Upload -> upload(cloudStorage, action)
                is SyncAction.Download -> download(cloudStorage, action.teamId, action.fileId, alwaysMerge = false)
                is SyncAction.Merge -> download(cloudStorage, action.teamId, action.fileId, alwaysMerge = true)
                is SyncAction.DeleteLocal -> deleteLocal(action.teamId)
                is SyncAction.DeleteRemote -> deleteRemote(cloudStorage, action)
                is SyncAction.MarkSynced -> markSynced(action)
            }
            report.add(action.teamId, result)?.let { return report.failed(it) }
        }

        if (sharedSync != null && index != null) {
            syncSharedTeams(sharedSync, index, report)?.let { return report.failed(it) }
        }

        // tombstones of teams no longer in the cloud are useless
        val remoteIds = privateRemote.map { it.id }.toSet()
        val now = clock.now()
        val newState = updateState { s -> s.copy(lastSyncAt = now, tombstones = s.tombstones.filterKeys { it in remoteIds }) }
        report.publish()
        return SyncStatus.Idle(newState.lastSyncAt, report.failures, report.merges, report.sharingNeedsConsent)
    }

    /**
     * @return the error stopping the sync, null if fine (including when the user has to allow the access to the
     * shared files first: the private sync worked)
     */
    private suspend fun syncSharedTeams(sharedSync: SharedTeamSync, index: SharedTeamsIndex, report: SyncReport): CloudError? {
        if (index.sharedTeamIds.isEmpty() && currentState().pendingSharedTrash.isEmpty()) return null
        sharedSync.trashPending().leftOrNull()?.let { return it.consentOr(report) }
        for (teamId in index.sharedTeamIds) {
            val result = sharedSync.syncTeam(teamId, index.teams.getValue(teamId))
            result.leftOrNull()?.takeIf { it.needsConsent() }?.let { return it.consentOr(report) }
            report.add(teamId, result)?.let { return it }
        }
        // what happened to the shared teams goes to the other devices, e.g. a lost access
        return sharedSync.pushIndex().leftOrNull()
    }

    // asking the access to the shared files needs the user: not an error, the private sync worked
    private fun CloudError.consentOr(report: SyncReport): CloudError? {
        if (!needsConsent()) return this
        report.sharingNeedsConsent = true
        return null
    }

    private fun CloudError.needsConsent() = this is CloudError.NeedsUserInteraction

    /**
     * What a sync did, team by team
     */
    private inner class SyncReport {
        val failures = mutableListOf<SyncFailure>()
        val merges = mutableListOf<TeamMerge>()
        val changedTeams = mutableSetOf<Uuid>()
        var sharingNeedsConsent = false

        /**
         * @return the error if it stops the sync
         */
        suspend fun add(teamId: Uuid, result: Either<CloudError, TeamSyncOutcome>): CloudError? {
            result.onRight { outcome ->
                if (outcome.changedLocally) changedTeams.add(teamId)
                outcome.merge?.let { merge ->
                    merges.add(merge)
                    _merges.tryEmit(merge)
                }
                outcome.notice?.let { _notices.tryEmit(it) }
            }
            val error = result.leftOrNull() ?: return null
            if (error.stopsSync()) return error
            val failure = SyncFailure(teamId, localTeams.name(teamId), error.message)
            println("Couldn't sync team ${failure.teamId} (${failure.teamName}): ${failure.reason}")
            failures.add(failure)
            return null
        }

        fun publish() {
            if (changedTeams.isNotEmpty()) _teamsChanged.tryEmit(changedTeams.toSet())
        }

        suspend fun failed(error: CloudError): SyncStatus {
            publish()
            return this@TeamSyncService.failed(error)
        }
    }

    private suspend fun upload(cloudStorage: CloudStorage, action: SyncAction.Upload): Either<CloudError, TeamSyncOutcome> {
        val entity = localTeams.get(action.teamId) ?: return TeamSyncOutcome().right()
        return uploadEntity(cloudStorage, entity, action.existingFileId).map { TeamSyncOutcome() }
    }

    private suspend fun uploadEntity(cloudStorage: CloudStorage, entity: TeamlyticsEntity, existingFileId: String?): Either<CloudError, Unit> {
        val bytes = json.encodeToString(entity).encodeToByteArray()
        val properties = remoteProperties(entity)
        var uploaded = cloudStorage.upload(fileName(entity.id), bytes, properties, existingFileId)
        if (existingFileId != null && uploaded.leftOrNull() is CloudError.NotFound) {
            // deleted in the meantime: create it again
            uploaded = cloudStorage.upload(fileName(entity.id), bytes, properties, existingFileId = null)
        }
        val file = uploaded.getOrElse { return it.left() }
        setSynced(entity, file.id)
        return Unit.right()
    }

    /**
     * Takes the cloud version. Merged with the local one if it changed too: always for [SyncAction.Merge], and for a
     * download if the team was changed locally since the sync was planned
     */
    private suspend fun download(cloudStorage: CloudStorage, teamId: Uuid, fileId: String, alwaysMerge: Boolean): Either<CloudError, TeamSyncOutcome> {
        val bytes = cloudStorage.download(fileId).getOrElse { return it.left() }
        val remote = try {
            json.decodeFromString<TeamlyticsEntity>(bytes.decodeToString())
        } catch (e: Exception) {
            return CloudError.Unknown("The Google Drive copy can't be read (${e.message})", e).left()
        }
        if (remote.id != teamId) return CloudError.Unknown("The Google Drive copy belongs to another team").left()

        val entry = currentState().entries[teamId]
        val merge = localTeams.applyRemote(remote, entry?.fingerprint, clock.now()) { local ->
            alwaysMerge || entry == null || !local.lastUpdatedAt.sameMillisAs(entry.syncedUpdatedAt)
        }.getOrElse { return it.left() }

        if (merge == null) {
            setSynced(remote, fileId)
            return TeamSyncOutcome(changedLocally = true).right()
        }
        // the cloud gets the merged version too
        uploadEntity(cloudStorage, merge.team, fileId).getOrElse { return it.left() }
        return TeamSyncOutcome(changedLocally = true, merge = TeamMerge(teamId, merge.team.name, merge.summary)).right()
    }

    private suspend fun markSynced(action: SyncAction.MarkSynced): Either<CloudError, TeamSyncOutcome> {
        val entry = currentState().entries[action.teamId]
        val upToDate = entry?.fingerprint != null && entry.remoteFileId == action.fileId
                && entry.syncedUpdatedAt.sameMillisAs(action.updatedAt)
        if (!upToDate) {
            // the fingerprint is the base of a future merge
            localTeams.get(action.teamId)?.let { setSynced(it, action.fileId) }
        }
        return TeamSyncOutcome().right()
    }

    private suspend fun setSynced(entity: TeamlyticsEntity, fileId: String) {
        val entry = SyncedEntry(fileId, entity.lastUpdatedAt, TeamFingerprint.of(entity))
        updateState { it.copy(entries = it.entries + (entity.id to entry)) }
    }

    private suspend fun deleteLocal(id: Uuid): Either<CloudError, TeamSyncOutcome> {
        localTeams.delete(id)
        updateState { it.copy(entries = it.entries - id) }
        return TeamSyncOutcome(changedLocally = true).right()
    }

    private suspend fun deleteRemote(cloudStorage: CloudStorage, action: SyncAction.DeleteRemote): Either<CloudError, TeamSyncOutcome> {
        val deleted = cloudStorage.delete(action.fileId)
        deleted.leftOrNull()?.takeIf { it !is CloudError.NotFound }?.let { return it.left() }
        updateState { state ->
            val entry = state.entries[action.teamId]
            // a duplicate file of a team still synced keeps its entry
            if (entry != null && entry.remoteFileId != action.fileId) state
            else state.copy(entries = state.entries - action.teamId, tombstones = state.tombstones - action.teamId)
        }
        return TeamSyncOutcome().right()
    }

    private suspend fun failed(error: CloudError) = SyncStatus.Failed(error, currentState().lastSyncAt)

    private fun CloudError.stopsSync() = when (this) {
        is CloudError.NotFound, is CloudError.Unknown, is CloudError.FileAccessDenied -> false
        else -> true
    }

    private suspend fun currentState(): SyncState = stateMutex.withLock {
        state ?: stateStore.load().also {
            state = it
            publishSharing(it)
        }
    }

    private suspend fun updateState(transform: (SyncState) -> SyncState): SyncState = stateMutex.withLock {
        val newState = transform(state ?: stateStore.load())
        stateStore.save(newState)
        state = newState
        publishSharing(newState)
        newState
    }

    private fun publishSharing(state: SyncState) {
        _sharing.value = state.sharedEntries.mapValues { (_, entry) -> entry.role }
        _formerSharing.value = state.formerlySharedRoles
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
