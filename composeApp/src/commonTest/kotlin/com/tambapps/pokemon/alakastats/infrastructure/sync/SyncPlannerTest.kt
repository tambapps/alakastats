package com.tambapps.pokemon.alakastats.infrastructure.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlin.uuid.Uuid

class SyncPlannerTest {

    private val id = Uuid.random()
    private val t1 = Instant.fromEpochMilliseconds(1_000)
    private val t2 = Instant.fromEpochMilliseconds(2_000)
    private val t3 = Instant.fromEpochMilliseconds(3_000)

    @Test
    fun newLocalTeamIsUploaded() {
        val actions = SyncPlanner.plan(listOf(LocalTeamVersion(id, t1)), emptyList(), SyncState())
        assertEquals(listOf(SyncAction.Upload(id, null)), actions)
    }

    @Test
    fun newRemoteTeamIsDownloaded() {
        val actions = SyncPlanner.plan(emptyList(), listOf(RemoteTeamVersion(id, "f", t1)), SyncState())
        assertEquals(listOf(SyncAction.Download(id, "f")), actions)
    }

    @Test
    fun newerLocalVersionWins() {
        val actions = SyncPlanner.plan(listOf(LocalTeamVersion(id, t2)), listOf(RemoteTeamVersion(id, "f", t1)), SyncState())
        assertEquals(listOf(SyncAction.Upload(id, "f")), actions)
    }

    @Test
    fun newerRemoteVersionWins() {
        val actions = SyncPlanner.plan(listOf(LocalTeamVersion(id, t1)), listOf(RemoteTeamVersion(id, "f", t2)), SyncState())
        assertEquals(listOf(SyncAction.Download(id, "f")), actions)
    }

    @Test
    fun sameVersionIsOnlyMarkedSynced() {
        val actions = SyncPlanner.plan(listOf(LocalTeamVersion(id, t1)), listOf(RemoteTeamVersion(id, "f", t1)), SyncState())
        assertEquals(listOf(SyncAction.MarkSynced(id, "f", t1)), actions)
    }

    @Test
    fun sameVersionAtTheMillisecondIsInSync() {
        // the cloud only keeps milliseconds
        val local = Instant.fromEpochSeconds(1, 123_456_789)
        val remote = Instant.fromEpochMilliseconds(1_123)
        val actions = SyncPlanner.plan(listOf(LocalTeamVersion(id, local)), listOf(RemoteTeamVersion(id, "f", remote)), SyncState())
        assertEquals(listOf(SyncAction.MarkSynced(id, "f", local)), actions)
    }

    @Test
    fun teamDeletedRemotelyIsDeletedLocally() {
        val state = SyncState(connected = true, entries = mapOf(id to SyncedEntry("f", t1)))
        val actions = SyncPlanner.plan(listOf(LocalTeamVersion(id, t1)), emptyList(), state)
        assertEquals(listOf(SyncAction.DeleteLocal(id)), actions)
    }

    @Test
    fun teamDeletedRemotelyButEditedLocallyIsUploadedAgain() {
        val state = SyncState(connected = true, entries = mapOf(id to SyncedEntry("f", t1)))
        val actions = SyncPlanner.plan(listOf(LocalTeamVersion(id, t2)), emptyList(), state)
        assertEquals(listOf(SyncAction.Upload(id, null)), actions)
    }

    @Test
    fun teamDeletedLocallyIsDeletedRemotely() {
        val state = SyncState(connected = true, tombstones = mapOf(id to t2))
        val actions = SyncPlanner.plan(emptyList(), listOf(RemoteTeamVersion(id, "f", t1)), state)
        assertEquals(listOf(SyncAction.DeleteRemote(id, "f")), actions)
    }

    @Test
    fun teamDeletedLocallyButEditedRemotelyAfterIsDownloaded() {
        val state = SyncState(connected = true, tombstones = mapOf(id to t2))
        val actions = SyncPlanner.plan(emptyList(), listOf(RemoteTeamVersion(id, "f", t3)), state)
        assertEquals(listOf(SyncAction.Download(id, "f")), actions)
    }

    @Test
    fun duplicateRemoteFilesKeepTheNewest() {
        val actions = SyncPlanner.plan(
            listOf(LocalTeamVersion(id, t1)),
            listOf(RemoteTeamVersion(id, "old", t1), RemoteTeamVersion(id, "new", t2)),
            SyncState(),
        )
        assertEquals(listOf(SyncAction.DeleteRemote(id, "old"), SyncAction.Download(id, "new")), actions)
    }

    @Test
    fun missingDatesAreTheOldest() {
        val actions = SyncPlanner.plan(listOf(LocalTeamVersion(id, null)), listOf(RemoteTeamVersion(id, "f", t1)), SyncState())
        assertEquals(listOf(SyncAction.Download(id, "f")), actions)
    }
}
