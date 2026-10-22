package com.tambapps.pokemon.alakastats.infrastructure.sync

import com.tambapps.pokemon.alakastats.infrastructure.cloud.ShareRole
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsPreviewEntity
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.Uuid

class SharedTeamsTest {

    private val clock = MutableClock(Instant.fromEpochSeconds(1_700_000_000, 123_456_789))
    private val drive = FakeDrive(clock)
    private val json = Json { ignoreUnknownKeys = true }

    private inner class Device(scope: CoroutineScope, val user: String, sharingConsent: Boolean = true) {
        val teams = InMemoryKStorage<Uuid, TeamlyticsEntity>()
        val previews = InMemoryKStorage<Uuid, TeamlyticsPreviewEntity>()
        val stateStore = InMemorySyncStateStore()
        val service = TeamSyncService(
            cloudStorage = drive.client(user, sharingConsent),
            tokenProvider = FakeTokenProvider(),
            teamsStorage = teams,
            previewsStorage = previews,
            stateStore = stateStore,
            pokepasteParser = PokepasteParser(),
            json = json,
            writeLock = TeamWriteLock(),
            scope = scope,
            clock = clock,
        )

        suspend fun save(team: TeamlyticsEntity) {
            teams.save(team)
            previews.save(preview(team))
        }

        suspend fun edit(id: Uuid, change: (TeamlyticsEntity) -> TeamlyticsEntity) {
            clock.now += 1.seconds
            save(change(teams.get(id)!!).copy(lastUpdatedAt = clock.now()))
        }

        /** what the app does when the user deletes a team */
        suspend fun delete(id: Uuid) {
            teams.delete(id)
            previews.delete(id)
            service.onLocalDelete(id)
        }

        suspend fun replays(id: Uuid) = teams.get(id)?.replays?.map { it.reference }?.toSet()
    }

    private suspend fun TestScope.device(user: String, sharingConsent: Boolean = true) =
        Device(backgroundScope, user, sharingConsent).also { it.service.connect() }

    private fun preview(team: TeamlyticsEntity) = TeamlyticsPreviewEntity(
        id = team.id, name = team.name, sdNames = team.sdNames, pokemons = emptyList(),
        nbReplays = team.replays.size, winrate = 0, lastUpdatedAt = team.lastUpdatedAt,
    )

    private val team = TeamlyticsEntity(
        id = Uuid.random(),
        name = "Rain",
        pokePaste = "",
        replays = listOf(replay("r1")),
        sdNames = listOf("me"),
        lastUpdatedAt = clock.now(),
    )

    private fun sharedFile() = drive.files.values.single { !it.isFolder }

    private fun sharedReplays() = json.decodeFromString<TeamlyticsEntity>(sharedFile().bytes.decodeToString()).replays.map { it.reference }.toSet()

    /** A shares the team with B, and B joins it */
    private suspend fun TestScope.sharedBetween(role: ShareRole = ShareRole.EDITOR): Pair<Device, Device> {
        val a = device("a")
        a.save(team)
        a.service.sync()
        assertTrue(a.service.shareTeam(team.id, "b", role).isRight())
        val b = device("b")
        drive.pick("b", sharedFile().id)
        assertEquals(listOf("Rain"), b.service.joinSharedTeams(listOf(sharedFile().id)).getOrNull())
        return a to b
    }

    @Test
    fun sharingMovesTheTeamToASharedFileOnlyTheOwnerCanShare() = runTest {
        val (a, _) = sharedBetween()

        val file = sharedFile()
        assertEquals("a", file.owner)
        assertEquals(mapOf("b" to ShareRole.EDITOR), file.members)
        assertFalse(file.writersCanShare)
        assertEquals(ShareRole.OWNER, a.service.sharing.value[team.id])
        // no private copy left
        assertTrue(drive.appDataOf("a").files.values.none { it.properties["teamId"] == team.id.toString() })
    }

    @Test
    fun editsOnBothSidesAreMerged() = runTest {
        val (a, b) = sharedBetween()

        a.edit(team.id) { it.copy(replays = it.replays + replay("fromA")) }
        b.edit(team.id) { it.copy(replays = it.replays + replay("fromB")) }
        a.service.sync()
        b.service.sync()
        a.service.sync()

        val expected = setOf("r1", "fromA", "fromB")
        assertEquals(expected, a.replays(team.id))
        assertEquals(expected, b.replays(team.id))
        assertEquals(expected, sharedReplays())
    }

    @Test
    fun ownersOtherDeviceKeepsTheMovedTeam() = runTest {
        val a1 = device("a")
        a1.save(team)
        a1.service.sync()
        val a2 = device("a")
        assertNotNull(a2.teams.get(team.id))

        a1.service.shareTeam(team.id, "b", ShareRole.EDITOR)
        a2.service.sync()

        // the private copy disappeared, but the index says the team moved: not deleted
        assertNotNull(a2.teams.get(team.id))
        assertEquals(ShareRole.OWNER, a2.service.sharing.value[team.id])
    }

    @Test
    fun joinedTeamComesToTheOtherDevicesOfTheCollaborator() = runTest {
        val (_, b1) = sharedBetween()
        b1.service.sync()

        val b2 = device("b")

        assertEquals(setOf("r1"), b2.replays(team.id))
        assertEquals(ShareRole.EDITOR, b2.service.sharing.value[team.id])
    }

    @Test
    fun viewerNeverChangesTheSharedTeam() = runTest {
        val (_, b) = sharedBetween(ShareRole.VIEWER)
        assertTrue(b.service.isReadOnly(team.id))
        val revisions = sharedFile().revisions.size

        b.edit(team.id) { it.copy(replays = it.replays + replay("fromViewer")) }
        b.service.sync()

        assertEquals(revisions, sharedFile().revisions.size)
        // the shared version wins
        assertEquals(setOf("r1"), b.replays(team.id))
    }

    @Test
    fun removedMemberKeepsAPrivateCopy() = runTest {
        val (a, b) = sharedBetween()
        val notices = mutableListOf<String>()
        backgroundScope.launch { b.service.notices.collect { notices.add(it) } }
        runCurrent()

        a.service.removeMember(team.id, "b")
        b.service.sync()
        runCurrent()

        assertNotNull(b.teams.get(team.id))
        assertNull(b.service.sharing.value[team.id])
        assertEquals(1, notices.size)
        // now a private team of B
        b.service.sync()
        assertTrue(drive.appDataOf("b").files.values.any { it.properties["teamId"] == team.id.toString() })
    }

    @Test
    fun ownerDeletingDeletesForEveryone() = runTest {
        val (a, b) = sharedBetween()

        a.delete(team.id)
        a.service.sync()
        b.service.sync()

        assertTrue(sharedFile().trashed)
        // B keeps a private copy
        assertNotNull(b.teams.get(team.id))
        assertNull(b.service.sharing.value[team.id])
    }

    @Test
    fun ownerDeletingOfflineTrashesAtTheNextSync() = runTest {
        val (a, _) = sharedBetween()
        val a2 = device("a")

        a.delete(team.id)
        a.service.sync()
        a2.service.sync()

        assertTrue(sharedFile().trashed)
        // deleted on the owner's other device too
        assertNull(a2.teams.get(team.id))
    }

    @Test
    fun leavingRemovesTheTeamFromTheCollaboratorsDevices() = runTest {
        val (_, b1) = sharedBetween()
        b1.service.sync()
        val b2 = device("b")
        assertNotNull(b2.teams.get(team.id))

        b1.delete(team.id)
        b1.service.sync()
        b2.service.sync()

        assertNull(b2.teams.get(team.id))
        assertFalse(sharedFile().trashed)
    }

    @Test
    fun stoppingTheSharingMakesItPrivateAgain() = runTest {
        val (a, b) = sharedBetween()

        a.service.stopSharing(team.id)
        b.service.sync()

        assertTrue(sharedFile().trashed)
        assertNull(a.service.sharing.value[team.id])
        assertTrue(drive.appDataOf("a").files.values.any { it.properties["teamId"] == team.id.toString() })
        assertNotNull(b.teams.get(team.id))
    }

    @Test
    fun concurrentWriteIsMergedInsteadOfLost() = runTest {
        val (a, b) = sharedBetween()
        b.service.sync()
        val fromB = json.decodeFromString<TeamlyticsEntity>(sharedFile().bytes.decodeToString())
            .let { it.copy(replays = it.replays + replay("fromB"), lastUpdatedAt = clock.now()) }
        // B writes the shared file between A's read and A's upload
        drive.beforeUpload = { fileId ->
            drive.beforeUpload = null
            drive.writeAs("b", fileId, json.encodeToString(fromB).encodeToByteArray())
        }

        a.edit(team.id) { it.copy(replays = it.replays + replay("fromA")) }
        a.service.sync()

        assertEquals(setOf("r1", "fromA", "fromB"), sharedReplays())
        assertEquals(setOf("r1", "fromA", "fromB"), a.replays(team.id))
    }

    @Test
    fun sharedTeamsWaitForConsentWithoutBlockingThePrivateSync() = runTest {
        val (_, b1) = sharedBetween()
        b1.service.sync()

        val b2 = device("b", sharingConsent = false)
        val status = assertIs<SyncStatus.Idle>(b2.service.status.value)

        assertTrue(status.sharingNeedsConsent)
        assertNull(b2.teams.get(team.id))
    }

    @Test
    fun disconnectedDeviceRemembersTheSharedRole() = runTest {
        val (_, b) = sharedBetween(ShareRole.VIEWER)

        b.service.disconnect()

        // no longer synced, but still shared in the cloud
        assertNull(b.service.sharing.value[team.id])
        assertEquals(ShareRole.VIEWER, b.service.formerSharing.value[team.id])
        assertEquals(TeamCloudState.DISCONNECTED, b.service.teamStates.value[team.id])
        // can't share from here while disconnected
        assertTrue(b.service.shareTeam(team.id, "c", ShareRole.EDITOR).isLeft())
    }
}
