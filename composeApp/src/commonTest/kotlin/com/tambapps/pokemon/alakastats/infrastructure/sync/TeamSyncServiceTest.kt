package com.tambapps.pokemon.alakastats.infrastructure.sync

import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudError
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsNotesEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsPreviewEntity
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.Uuid

class TeamSyncServiceTest {

    // nanoseconds on purpose: the cloud only keeps milliseconds
    private val clock = MutableClock(Instant.fromEpochSeconds(1_700_000_000, 123_456_789))
    private val cloud = FakeCloudStorage(clock)
    private val json = Json { ignoreUnknownKeys = true }

    private inner class Device(scope: CoroutineScope, tokenProvider: FakeTokenProvider = FakeTokenProvider()) {
        val teams = InMemoryKStorage<Uuid, TeamlyticsEntity>()
        val previews = InMemoryKStorage<Uuid, TeamlyticsPreviewEntity>()
        val stateStore = InMemorySyncStateStore()
        val service = TeamSyncService(
            cloudStorage = cloud,
            tokenProvider = tokenProvider,
            teamsStorage = teams,
            previewsStorage = previews,
            stateStore = stateStore,
            pokepasteParser = PokepasteParser(),
            json = json,
            writeLock = TeamWriteLock(),
            scope = scope,
            clock = clock,
        )

        /** what KStoreTeamlyticsRepository.save does */
        suspend fun save(team: TeamlyticsEntity) {
            teams.save(team)
            previews.save(preview(team))
        }

        suspend fun delete(id: Uuid) {
            teams.delete(id)
            previews.delete(id)
            service.onLocalDelete(id)
        }
    }

    private fun TestScope.device(tokenProvider: FakeTokenProvider = FakeTokenProvider()) = Device(backgroundScope, tokenProvider)

    private fun team(name: String = "Team", id: Uuid = Uuid.random()) = TeamlyticsEntity(
        id = id,
        name = name,
        pokePaste = "",
        replays = emptyList(),
        sdNames = listOf("me"),
        lastUpdatedAt = clock.now(),
    )

    private fun preview(team: TeamlyticsEntity) = TeamlyticsPreviewEntity(
        id = team.id,
        name = team.name,
        sdNames = team.sdNames,
        pokemons = emptyList(),
        nbReplays = team.replays.size,
        winrate = 0,
        lastUpdatedAt = team.lastUpdatedAt,
        localOnly = team.localOnly,
    )

    private fun tick() {
        clock.now = clock.now + 1.seconds
    }

    @Test
    fun teamCreatedOnOneDeviceIsDownloadedOnTheOther() = runTest {
        val a = device()
        val b = device()
        val team = team("Rain")
        a.save(team)

        a.service.connect()
        b.service.connect()

        val downloaded = assertNotNull(b.teams.get(team.id))
        assertEquals("Rain", downloaded.name)
        // the date of the remote version is kept, not the download date
        assertEquals(team.lastUpdatedAt, downloaded.lastUpdatedAt)
        val preview = assertNotNull(b.previews.get(team.id))
        assertEquals("Rain", preview.name)
        assertEquals(team.lastUpdatedAt, preview.lastUpdatedAt)
    }

    @Test
    fun syncWithoutChangesUploadsNothing() = runTest {
        val a = device()
        a.save(team())
        a.service.connect()
        val uploads = cloud.uploads

        a.service.sync()
        a.service.sync()

        assertEquals(uploads, cloud.uploads)
    }

    @Test
    fun concurrentEditsLastWriteWins() = runTest {
        val a = device()
        val b = device()
        val team = team("Original")
        a.save(team)
        a.service.connect()
        b.service.connect()

        tick()
        a.save(team.copy(name = "Edited on A", lastUpdatedAt = clock.now()))
        tick()
        b.save(team.copy(name = "Edited on B", lastUpdatedAt = clock.now()))

        a.service.sync()
        b.service.sync()
        a.service.sync()

        assertEquals("Edited on B", a.teams.get(team.id)?.name)
        assertEquals("Edited on B", b.teams.get(team.id)?.name)
    }

    @Test
    fun olderRemoteVersionDoesNotOverrideLocalEdit() = runTest {
        val a = device()
        val b = device()
        val team = team("Original")
        a.save(team)
        a.service.connect()
        b.service.connect()

        tick()
        b.save(team.copy(name = "Newer on B", lastUpdatedAt = clock.now()))
        a.service.sync()
        b.service.sync()

        assertEquals("Newer on B", b.teams.get(team.id)?.name)
    }

    @Test
    fun deletionIsPropagated() = runTest {
        val a = device()
        val b = device()
        val team = team()
        a.save(team)
        a.service.connect()
        b.service.connect()

        tick()
        a.delete(team.id)
        a.service.sync()
        b.service.sync()

        assertTrue(cloud.files.isEmpty())
        assertNull(b.teams.get(team.id))
        assertNull(b.previews.get(team.id))
        assertTrue(a.stateStore.state.tombstones.isEmpty())
    }

    @Test
    fun editAfterDeletionOnAnotherDeviceWins() = runTest {
        val a = device()
        val b = device()
        val team = team("Original")
        a.save(team)
        a.service.connect()
        b.service.connect()

        tick()
        a.delete(team.id)
        tick()
        b.save(team.copy(name = "Edited on B", lastUpdatedAt = clock.now()))
        b.service.sync()
        a.service.sync()

        assertEquals("Edited on B", a.teams.get(team.id)?.name)
    }

    @Test
    fun localEditAfterRemoteDeletionIsUploadedAgain() = runTest {
        val a = device()
        val b = device()
        val team = team("Original")
        a.save(team)
        a.service.connect()
        b.service.connect()

        tick()
        a.delete(team.id)
        a.service.sync()
        tick()
        b.save(team.copy(name = "Edited on B", lastUpdatedAt = clock.now()))
        b.service.sync()

        assertEquals("Edited on B", b.teams.get(team.id)?.name)
        assertEquals(1, cloud.files.size)
    }

    @Test
    fun disconnectedDeviceNeverCallsTheCloud() = runTest {
        val a = device()
        a.save(team())
        a.delete(Uuid.random())

        val status = a.service.sync()

        assertIs<SyncStatus.Disconnected>(status)
        assertEquals(0, cloud.calls)
        assertTrue(a.stateStore.state.tombstones.isEmpty())
    }

    @Test
    fun cancelledConnectionStaysDisconnected() = runTest {
        val a = device(FakeTokenProvider().apply { interactiveError = CloudError.Cancelled() })

        val result = a.service.connect()

        assertIs<CloudError.Cancelled>(result.leftOrNull())
        assertEquals(false, a.stateStore.state.connected)
        assertEquals(0, cloud.calls)
    }

    @Test
    fun fullStorageStopsTheSyncAndKeepsLocalTeams() = runTest {
        val a = device()
        val team = team()
        a.save(team)
        cloud.uploadError = CloudError.StorageFull()

        a.service.connect()

        val status = assertIs<SyncStatus.Failed>(a.service.status.value)
        assertIs<CloudError.StorageFull>(status.error)
        assertNotNull(a.teams.get(team.id))
        assertNull(a.stateStore.state.lastSyncAt)

        // once there is space again, the next sync uploads the team
        cloud.uploadError = null
        assertIs<SyncStatus.Idle>(a.service.sync())
        assertEquals(1, cloud.files.size)
    }

    @Test
    fun offlineSyncFailsWithoutTouchingLocalTeams() = runTest {
        val a = device()
        val team = team()
        a.save(team)
        a.service.connect()
        cloud.listError = CloudError.Network("offline")

        tick()
        a.save(team.copy(name = "Edited offline", lastUpdatedAt = clock.now()))
        val status = a.service.sync()

        assertIs<CloudError.Network>(assertIs<SyncStatus.Failed>(status).error)
        assertEquals("Edited offline", a.teams.get(team.id)?.name)

        cloud.listError = null
        a.service.sync()
        val uploaded = json.decodeFromString<TeamlyticsEntity>(cloud.files.values.single().bytes.decodeToString())
        assertEquals("Edited offline", uploaded.name)
    }

    @Test
    fun revokedAccessAsksForReconnection() = runTest {
        val a = device()
        a.service.connect()
        cloud.listError = CloudError.AccessRevoked()

        val status = assertIs<SyncStatus.Failed>(a.service.sync())

        assertTrue(status.needsReconnection)
    }

    @Test
    fun brokenRemoteFileDoesNotStopTheOthers() = runTest {
        val a = device()
        val b = device()
        val team = team("Good")
        a.save(team)
        a.service.connect()
        val brokenId = Uuid.random()
        cloud.files["broken"] = FakeCloudStorage.StoredFile(
            name = TeamSyncService.fileName(brokenId),
            bytes = "not json".encodeToByteArray(),
            properties = mapOf("teamId" to brokenId.toString(), "schema" to "1", "updatedAt" to "1"),
            modifiedAt = clock.now(),
        )

        b.service.connect()
        val status = assertIs<SyncStatus.Idle>(b.service.status.value)

        val failure = status.failures.single()
        assertEquals(brokenId, failure.teamId)
        // not on this device, so no name to show
        assertNull(failure.teamName)
        assertContains(failure.reason, "can't be read")
        assertNotNull(b.teams.get(team.id))
    }

    @Test
    fun failedUploadGivesTheTeamNameAndTheReason() = runTest {
        val a = device()
        a.save(team("Rain"))
        a.service.connect()
        tick()
        a.save(a.teams.listEntities().single().copy(lastUpdatedAt = clock.now()))
        cloud.uploadError = CloudError.Unknown("Google Drive error 400: Bad request")

        val failure = assertIs<SyncStatus.Idle>(a.service.sync()).failures.single()

        assertEquals("Rain", failure.teamName)
        assertEquals("Google Drive error 400: Bad request", failure.reason)
    }

    @Test
    fun filesFromANewerSchemaAreIgnored() = runTest {
        val a = device()
        cloud.files["future"] = FakeCloudStorage.StoredFile(
            name = "team-future.json",
            bytes = "{}".encodeToByteArray(),
            properties = mapOf("teamId" to Uuid.random().toString(), "schema" to "2"),
            modifiedAt = clock.now(),
        )

        a.service.connect()

        assertTrue(a.teams.listIds().isEmpty())
        assertIs<SyncStatus.Idle>(a.service.status.value)
    }

    @Test
    fun disconnectionKeepsLocalAndRemoteTeams() = runTest {
        val tokenProvider = FakeTokenProvider()
        val a = device(tokenProvider)
        val team = team()
        a.save(team)
        a.service.connect()

        a.service.disconnect()

        assertTrue(tokenProvider.signedOut)
        // only which teams were synced is kept
        assertEquals(SyncState(formerlyLinkedTeams = setOf(team.id)), a.stateStore.state)
        assertIs<SyncStatus.Disconnected>(a.service.status.value)
        assertNotNull(a.teams.get(team.id))
        assertEquals(1, cloud.files.size)
    }

    @Test
    fun burstOfRequestsGivesASingleSync() = runTest {
        val a = device()
        a.service.connect()
        val calls = cloud.calls

        repeat(5) { a.service.requestSync() }
        advanceTimeBy(1.seconds)
        runCurrent()
        assertEquals(calls, cloud.calls)

        advanceTimeBy(3.seconds)
        runCurrent()
        // a single list call: nothing to upload or download
        assertEquals(calls + 1, cloud.calls)
    }

    @Test
    fun teamsChangedIsEmittedOnDownload() = runTest {
        val a = device()
        val b = device()
        a.save(team())
        a.service.connect()
        var emissions = 0
        backgroundScope.launch { b.service.teamsChanged.collect { emissions++ } }
        runCurrent()

        b.service.connect()
        runCurrent()

        assertEquals(1, emissions)
    }

    /** two devices synced on the same team */
    private suspend fun TestScope.syncedDevices(team: TeamlyticsEntity): Pair<Device, Device> {
        val a = device()
        val b = device()
        a.save(team)
        a.service.connect()
        b.service.connect()
        return a to b
    }

    private suspend fun Device.edit(id: Uuid, change: (TeamlyticsEntity) -> TeamlyticsEntity) {
        tick()
        save(change(teams.get(id)!!).copy(lastUpdatedAt = clock.now()))
    }

    @Test
    fun replaysAddedOfflineOnBothDevicesAreAllKept() = runTest {
        val team = team("Rain").copy(replays = listOf(replay("r1")))
        val (a, b) = syncedDevices(team)

        a.edit(team.id) { it.copy(replays = it.replays + replay("fromA")) }
        b.edit(team.id) { it.copy(replays = it.replays + replay("fromB")) }
        a.service.sync()
        val status = assertIs<SyncStatus.Idle>(b.service.sync())
        a.service.sync()

        val expected = setOf("r1", "fromA", "fromB")
        assertEquals(expected, b.teams.get(team.id)!!.replays.map { it.reference }.toSet())
        assertEquals(expected, a.teams.get(team.id)!!.replays.map { it.reference }.toSet())
        val merge = status.merges.single()
        assertEquals("Rain", merge.teamName)
        assertEquals(1, merge.summary.replaysAdded)
    }

    @Test
    fun notesEditedOnBothDevicesAreKeptTwice() = runTest {
        val team = team().copy(notes = TeamlyticsNotesEntity("plan", emptyMap()))
        val (a, b) = syncedDevices(team)

        a.edit(team.id) { it.copy(notes = TeamlyticsNotesEntity("plan from A", emptyMap())) }
        b.edit(team.id) { it.copy(notes = TeamlyticsNotesEntity("plan from B", emptyMap())) }
        a.service.sync()
        b.service.sync()
        a.service.sync()

        val expected = "plan from B${TeamMerger.SEPARATOR}plan from A"
        assertEquals(expected, b.teams.get(team.id)!!.notes?.teamNotes)
        assertEquals(expected, a.teams.get(team.id)!!.notes?.teamNotes)
    }

    @Test
    fun replayDeletedOnOneDeviceButNotedOnTheOtherIsKept() = runTest {
        val team = team().copy(replays = listOf(replay("r1"), replay("r2")))
        val (a, b) = syncedDevices(team)

        a.edit(team.id) { it.copy(replays = it.replays.filter { replay -> replay.reference != "r2" }) }
        b.edit(team.id) { it.copy(replays = it.replays.map { replay -> if (replay.reference == "r2") replay.copy(notes = "great game") else replay }) }
        a.service.sync()
        b.service.sync()

        assertEquals("great game", b.teams.get(team.id)!!.replays.single { it.reference == "r2" }.notes)
    }

    @Test
    fun mergedTeamIsNotSyncedAgainWithoutChanges() = runTest {
        val team = team()
        val (a, b) = syncedDevices(team)
        a.edit(team.id) { it.copy(replays = listOf(replay("fromA"))) }
        b.edit(team.id) { it.copy(replays = listOf(replay("fromB"))) }
        a.service.sync()
        b.service.sync()
        val uploads = cloud.uploads

        assertIs<SyncStatus.Idle>(b.service.sync()).also { assertTrue(it.merges.isEmpty()) }

        assertEquals(uploads, cloud.uploads)
        assertNotNull(b.stateStore.state.entries[team.id]?.fingerprint)
    }

    @Test
    fun mergesAreEmittedToTellTheUser() = runTest {
        val team = team()
        val (a, b) = syncedDevices(team)
        val merges = mutableListOf<TeamMerge>()
        backgroundScope.launch { b.service.merges.collect { merges.add(it) } }
        runCurrent()

        a.edit(team.id) { it.copy(name = "From A") }
        b.edit(team.id) { it.copy(replays = listOf(replay("fromB"))) }
        a.service.sync()
        b.service.sync()
        runCurrent()

        // the name changed on A only: taken by B
        assertEquals("From A", merges.single().teamName)
    }

    @Test
    fun localChangeMadeDuringASyncIsMergedInsteadOfOverwritten() = runTest {
        val team = team().copy(replays = listOf(replay("r1")))
        val (a, b) = syncedDevices(team)
        a.edit(team.id) { it.copy(replays = it.replays + replay("fromA")) }
        a.service.sync()
        // B's sync plans a download (B hasn't changed the team), but B saves while it downloads
        cloud.onDownload = {
            cloud.onDownload = null
            b.edit(team.id) { it.copy(replays = it.replays + replay("fromB")) }
        }

        val status = assertIs<SyncStatus.Idle>(b.service.sync())

        assertEquals(setOf("r1", "fromA", "fromB"), b.teams.get(team.id)!!.replays.map { it.reference }.toSet())
        assertEquals(1, status.merges.size)
    }

    @Test
    fun teamStatesFollowTheSync() = runTest {
        val a = device()
        val team = team()
        a.save(team)
        // not connected: nothing to show
        assertTrue(a.service.teamStates.value.isEmpty())

        a.service.connect()
        assertEquals(TeamCloudState.SYNCED, a.service.teamStates.value[team.id])

        // changed locally: shown pending right away, before the sync
        a.edit(team.id) { it.copy(name = "Edited") }
        a.service.requestSync()
        runCurrent()
        assertEquals(TeamCloudState.PENDING, a.service.teamStates.value[team.id])

        cloud.uploadError = CloudError.Unknown("Google Drive error 400")
        a.service.sync()
        assertEquals(TeamCloudState.FAILED, a.service.teamStates.value[team.id])

        cloud.uploadError = null
        a.service.sync()
        assertEquals(TeamCloudState.SYNCED, a.service.teamStates.value[team.id])

        // disconnected: the team still has a copy in the cloud, no longer kept up to date
        a.service.disconnect()
        assertEquals(TeamCloudState.DISCONNECTED, a.service.teamStates.value[team.id])

        a.service.connect()
        assertEquals(TeamCloudState.SYNCED, a.service.teamStates.value[team.id])
        assertTrue(a.stateStore.state.formerlyLinkedTeams.isEmpty())
    }

    @Test
    fun teamKeptOnThisDeviceOnlyIsNeverUploaded() = runTest {
        val a = device()
        val team = team().copy(localOnly = true)
        a.save(team)

        a.service.connect()
        a.service.sync()

        assertTrue(cloud.files.isEmpty())
        assertNotNull(a.teams.get(team.id))
        // no cloud icon for it
        assertNull(a.service.teamStates.value[team.id])
    }

    @Test
    fun keepingOnThisDeviceOnlyRemovesItFromTheCloudAndTheOtherDevices() = runTest {
        val team = team("Rain")
        val (a, b) = syncedDevices(team)

        a.edit(team.id) { it.copy(localOnly = true) }
        a.service.sync()
        b.service.sync()

        assertTrue(cloud.files.isEmpty())
        assertNotNull(a.teams.get(team.id))
        assertNull(b.teams.get(team.id))
    }

    @Test
    fun syncingAgainSendsItBackToTheOtherDevices() = runTest {
        val team = team("Rain")
        val (a, b) = syncedDevices(team)
        a.edit(team.id) { it.copy(localOnly = true) }
        a.service.sync()
        b.service.sync()

        a.edit(team.id) { it.copy(localOnly = false) }
        a.service.sync()
        b.service.sync()

        assertEquals(1, cloud.files.size)
        assertEquals("Rain", b.teams.get(team.id)?.name)
        assertEquals(TeamCloudState.SYNCED, a.service.teamStates.value[team.id])
    }

    @Test
    fun teamNeverSyncedHasNoStateOnceDisconnected() = runTest {
        val a = device()
        a.service.connect()
        a.service.disconnect()
        val team = team()
        a.save(team)
        a.service.requestSync()
        runCurrent()

        assertNull(a.service.teamStates.value[team.id])
    }
}
