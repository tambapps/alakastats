package com.tambapps.pokemon.alakastats.infrastructure.sync

import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.GamePlanEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.MatchupNotesEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsNotesEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlin.uuid.Uuid

class TeamMergerTest {

    private val now = Instant.fromEpochSeconds(1_800_000_000)
    private val base = TeamlyticsEntity(
        id = Uuid.random(),
        name = "Rain",
        pokePaste = "Pelipper",
        replays = listOf(replay("r1", nextBattleRef = "r2"), replay("r2")),
        sdNames = listOf("me"),
        notes = TeamlyticsNotesEntity("base notes", mapOf("Pelipper" to "set rain")),
        lastUpdatedAt = Instant.fromEpochSeconds(1_700_000_000),
        matchupNotes = listOf(plan("vs Sun")),
        format = "VGC",
    )

    private fun plan(name: String, description: String = "lead Pelipper") =
        MatchupNotesEntity(Uuid.random(), name, null, listOf(GamePlanEntity(description)))

    private fun merge(local: TeamlyticsEntity, remote: TeamlyticsEntity, base: TeamlyticsEntity? = this.base, localIsNewer: Boolean = true) =
        TeamMerger.merge(base?.let { TeamFingerprint.of(it) }, local, remote, localIsNewer, now)

    @Test
    fun replaysAddedOnBothSidesAreAllKept() {
        val local = base.copy(replays = base.replays + replay("local"))
        val remote = base.copy(replays = base.replays + replay("remote"))

        val result = merge(local, remote)

        assertEquals(listOf("r1", "r2", "local", "remote"), result.team.replays.map { it.reference })
        assertEquals(1, result.summary.replaysAdded)
    }

    @Test
    fun replayDeletedOnOneSideIsDeleted() {
        val local = base.copy(replays = listOf(replay("r1", nextBattleRef = "r2")))
        val remote = base.copy(name = "Rain v2")

        val result = merge(local, remote)

        assertEquals(listOf("r1"), result.team.replays.map { it.reference })
        assertEquals("Rain v2", result.team.name)
    }

    @Test
    fun replayDeletedByTheOtherSideIsCountedAsRemoved() {
        val local = base.copy(name = "Rain v2")
        val remote = base.copy(replays = listOf(replay("r1", nextBattleRef = "r2")))

        val result = merge(local, remote)

        assertEquals(listOf("r1"), result.team.replays.map { it.reference })
        assertEquals(1, result.summary.replaysRemoved)
    }

    @Test
    fun replayDeletedOnOneSideButNotedOnTheOtherIsKept() {
        val local = base.copy(replays = listOf(replay("r1", nextBattleRef = "r2")))
        val remote = base.copy(replays = listOf(replay("r1", nextBattleRef = "r2"), replay("r2", notes = "misplay turn 3")))

        val result = merge(local, remote)

        assertEquals("misplay turn 3", result.team.replays.single { it.reference == "r2" }.notes)
    }

    @Test
    fun replayFromTheOtherSideGoesAtItsSpotInTheChain() {
        // battles a -> b -> c: b was only added on the other side
        val synced = base.copy(replays = listOf(replay("a", nextBattleRef = "b"), replay("c")))
        val local = synced.copy(name = "Rain v2")
        val remote = synced.copy(replays = listOf(replay("a", nextBattleRef = "b"), replay("c"), replay("b", nextBattleRef = "c")))

        val result = merge(local, remote, base = synced)

        assertEquals(listOf("a", "b", "c"), result.team.replays.map { it.reference })
    }

    @Test
    fun replayNotesWrittenOnBothSidesAreKeptTwice() {
        val local = base.copy(replays = listOf(base.replays[0].copy(notes = "local note"), base.replays[1]))
        val remote = base.copy(replays = listOf(base.replays[0].copy(notes = "remote note"), base.replays[1]))

        val result = merge(local, remote)

        assertEquals("local note${TeamMerger.SEPARATOR}remote note", result.team.replays[0].notes)
        assertEquals(1, result.summary.textsKeptTwice)
    }

    @Test
    fun teamNotesEditedOnBothSidesAreKeptTwice() {
        val local = base.copy(notes = TeamlyticsNotesEntity("local notes", base.notes!!.pokemonNotes))
        val remote = base.copy(notes = TeamlyticsNotesEntity("remote notes", base.notes!!.pokemonNotes))

        val result = merge(local, remote)

        assertEquals("local notes${TeamMerger.SEPARATOR}remote notes", result.team.notes?.teamNotes)
        assertEquals("set rain", result.team.notes?.pokemonNotes?.get("Pelipper"))
    }

    @Test
    fun notesEditedOnOneSideOnlyAreTaken() {
        val local = base.copy(name = "Rain v2")
        val remote = base.copy(notes = TeamlyticsNotesEntity("base notes", mapOf("Pelipper" to "set rain turn 1")))

        val result = merge(local, remote)

        assertEquals("set rain turn 1", result.team.notes?.pokemonNotes?.get("Pelipper"))
        assertEquals("base notes", result.team.notes?.teamNotes)
        assertEquals(0, result.summary.textsKeptTwice)
    }

    @Test
    fun alreadyMergedTextIsNotDuplicated() {
        // e.g. merged locally, then the upload failed: the next sync merges again
        val merged = "local notes${TeamMerger.SEPARATOR}remote notes"
        val local = base.copy(notes = TeamlyticsNotesEntity(merged, emptyMap()))
        val remote = base.copy(notes = TeamlyticsNotesEntity("remote notes", emptyMap()))

        val result = merge(local, remote)

        assertEquals(merged, result.team.notes?.teamNotes)
    }

    @Test
    fun plansAddedOnBothSidesAreAllKept() {
        val localPlan = plan("vs Trick Room")
        val remotePlan = plan("vs Tailwind")
        val local = base.copy(matchupNotes = base.matchupNotes!! + localPlan)
        val remote = base.copy(matchupNotes = base.matchupNotes!! + remotePlan)

        val result = merge(local, remote)

        assertEquals(listOf("vs Sun", "vs Trick Room", "vs Tailwind"), result.team.matchupNotes?.map { it.name })
        assertEquals(1, result.summary.plansAdded)
    }

    @Test
    fun planEditedOnBothSidesIsKeptInBothVersions() {
        val basePlan = base.matchupNotes!!.single()
        val local = base.copy(matchupNotes = listOf(basePlan.copy(gamePlans = listOf(GamePlanEntity("local plan")))))
        val remote = base.copy(matchupNotes = listOf(basePlan.copy(gamePlans = listOf(GamePlanEntity("remote plan")))))

        val result = merge(local, remote)

        val plans = result.team.matchupNotes!!
        assertEquals(listOf("vs Sun", "vs Sun${TeamMerger.OTHER_VERSION_SUFFIX}"), plans.map { it.name })
        assertEquals(listOf("local plan", "remote plan"), plans.map { it.gamePlans.single().description })
        assertNotEquals(plans[0].id, plans[1].id)
        assertEquals(1, result.summary.plansKeptTwice)

        // merged again (e.g. after a failed upload): no third version
        assertEquals(2, merge(result.team, remote).team.matchupNotes!!.size)
    }

    @Test
    fun nameChangedOnBothSidesTakesTheNewest() {
        val local = base.copy(name = "Local")
        val remote = base.copy(name = "Remote")

        assertEquals("Local", merge(local, remote, localIsNewer = true).team.name)
        assertEquals("Remote", merge(local, remote, localIsNewer = false).team.name)
    }

    @Test
    fun pasteChangedOnOneSideIsTaken() {
        val local = base.copy(notes = TeamlyticsNotesEntity("new notes", emptyMap()))
        val remote = base.copy(pokePaste = "Pelipper\nArchaludon")

        assertEquals("Pelipper\nArchaludon", merge(local, remote, localIsNewer = true).team.pokePaste)
    }

    @Test
    fun sdNamesAreMergedAsSets() {
        val local = base.copy(sdNames = listOf("me", "alt"))
        val remote = base.copy(sdNames = listOf("main"))

        // "me" removed by the remote side, "alt" and "main" added
        assertEquals(listOf("alt", "main"), merge(local, remote).team.sdNames)
    }

    @Test
    fun withoutBaseEverythingIsKept() {
        val local = base.copy(replays = listOf(replay("r1")), notes = TeamlyticsNotesEntity("local", emptyMap()))
        val remote = base.copy(replays = listOf(replay("r2")), notes = TeamlyticsNotesEntity("remote", emptyMap()))

        val result = merge(local, remote, base = null)

        assertEquals(listOf("r1", "r2"), result.team.replays.map { it.reference })
        assertEquals("local${TeamMerger.SEPARATOR}remote", result.team.notes?.teamNotes)
    }

    @Test
    fun mergedVersionIsDatedNow() {
        val result = merge(base.copy(name = "a"), base.copy(name = "b"))

        assertEquals(now, result.team.lastUpdatedAt)
        assertEquals(base.id, result.team.id)
    }

    @Test
    fun noNotesOnBothSidesStayNull() {
        val result = merge(base.copy(notes = null), base.copy(notes = null))

        assertNull(result.team.notes)
    }
}
