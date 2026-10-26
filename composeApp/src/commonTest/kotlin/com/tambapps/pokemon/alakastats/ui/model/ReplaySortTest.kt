package com.tambapps.pokemon.alakastats.ui.model

import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.domain.model.Player
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.TeamPreview
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsData
import com.tambapps.pokemon.alakastats.domain.model.UserName
import com.tambapps.pokemon.pokepaste.parser.PokePaste
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

class ReplaySortTest {

    @Test
    fun defaultSortIsNewestFirst() {
        val sort = ReplaySort()
        assertEquals(ReplaySortCriterion.DATE, sort.criterion)
        assertTrue(sort.descending)
        assertTrue(sort.isDefault)

        val team = team(
            replay("a", uploadTime = 200),
            replay("b", uploadTime = 300),
            replay("c", uploadTime = 100),
        )
        assertEquals(listOf("b", "a", "c"), team.sortedReplays(sort).references())
    }

    @Test
    fun oldestFirstIsTheReverse() {
        val team = team(
            replay("a", uploadTime = 200),
            replay("b", uploadTime = 300),
            replay("c", uploadTime = 100),
            replay("d", uploadTime = 200),
        )
        val newestFirst = team.sortedReplays(ReplaySort()).references()
        val oldestFirst = team.sortedReplays(ReplaySort(ReplaySortCriterion.DATE, descending = false)).references()
        assertEquals(listOf("b", "d", "a", "c"), newestFirst)
        assertEquals(newestFirst.reversed(), oldestFirst)
    }

    @Test
    fun sameDatesAreOrderedFromTheLastAdded() {
        val team = team(
            replay("a", uploadTime = 100),
            replay("b", uploadTime = 100),
            replay("c", uploadTime = 100),
        )
        assertEquals(listOf("c", "b", "a"), team.sortedReplays(ReplaySort()).references())
    }

    @Test
    fun missingDatesAreLastInBothDirections() {
        val team = team(
            replay("noDate1", uploadTime = 0),
            replay("old", uploadTime = 100),
            replay("noDate2", uploadTime = 0),
            replay("new", uploadTime = 200),
        )
        assertEquals(
            listOf("new", "old", "noDate2", "noDate1"),
            team.sortedReplays(ReplaySort()).references()
        )
        assertEquals(
            listOf("old", "new", "noDate1", "noDate2"),
            team.sortedReplays(ReplaySort(ReplaySortCriterion.DATE, descending = false)).references()
        )
    }

    @Test
    fun outcomeSortPutsUnknownLastAndTiesNewestFirst() {
        val team = team(
            replay("oldWin", uploadTime = 100, winner = ME),
            replay("loss", uploadTime = 300, winner = "opp"),
            replay("unknown", uploadTime = 400, winner = null),
            replay("newWin", uploadTime = 200, winner = ME),
        )
        assertEquals(
            listOf("newWin", "oldWin", "loss", "unknown"),
            team.sortedReplays(ReplaySort(ReplaySortCriterion.OUTCOME)).references()
        )
        assertEquals(
            listOf("loss", "newWin", "oldWin", "unknown"),
            team.sortedReplays(ReplaySort(ReplaySortCriterion.OUTCOME, descending = false)).references()
        )
    }

    @Test
    fun opponentSortIsAlphabeticalIgnoringCase() {
        val team = team(
            replay("z", uploadTime = 100, opponent = "zed"),
            replay("a", uploadTime = 200, opponent = "Alice"),
            replay("b", uploadTime = 300, opponent = "bob"),
        )
        val sort = ReplaySort(ReplaySortCriterion.OPPONENT)
        // A to Z is the natural order of names
        assertEquals(false, sort.descending)
        assertEquals(listOf("a", "b", "z"), team.sortedReplays(sort).references())
    }

    @Test
    fun eloSortPutsMissingEloLast() {
        val team = team(
            replay("noElo", uploadTime = 400, myElo = null),
            replay("low", uploadTime = 100, myElo = 1100),
            replay("high", uploadTime = 200, myElo = 1500),
            replay("lowNewer", uploadTime = 300, myElo = 1100),
        )
        assertEquals(
            listOf("high", "lowNewer", "low", "noElo"),
            team.sortedReplays(ReplaySort(ReplaySortCriterion.ELO)).references()
        )
        assertEquals(
            listOf("lowNewer", "low", "high", "noElo"),
            team.sortedReplays(ReplaySort(ReplaySortCriterion.ELO, descending = false)).references()
        )
    }

    @Test
    fun addedSortFollowsTheStoredOrder() {
        val team = team(
            replay("first", uploadTime = 300),
            replay("second", uploadTime = 100),
            replay("third", uploadTime = 200),
        )
        assertEquals(
            listOf("third", "second", "first"),
            team.sortedReplays(ReplaySort(ReplaySortCriterion.ADDED)).references()
        )
        assertEquals(
            listOf("first", "second", "third"),
            team.sortedReplays(ReplaySort(ReplaySortCriterion.ADDED, descending = false)).references()
        )
    }

    @Test
    fun sortingKeepsTheTeamAsIs() {
        val team = team(
            replay("a", uploadTime = 100),
            replay("b", uploadTime = 200),
        )
        val replays = team.replays
        team.sortedReplays(ReplaySort())
        assertEquals(replays, team.replays)
        assertEquals(listOf("a", "b"), team.replays.references())
    }

    private fun List<ReplayAnalytics>.references() = map { it.reference }

    private fun team(vararg replays: ReplayAnalytics) = Teamlytics(
        id = Uuid.random(),
        name = "Team",
        pokePaste = PokePaste(emptyList()),
        replays = replays.toList(),
        sdNames = listOf(UserName(ME)),
        lastUpdatedAt = Instant.fromEpochMilliseconds(0),
        data = TeamlyticsData(emptyMap()),
        notes = null,
        matchupPlans = emptyList(),
        format = Format.NONE,
    )

    private fun replay(
        reference: String,
        uploadTime: Long,
        winner: String? = ME,
        opponent: String = "opp",
        myElo: Int? = null,
    ) = ReplayAnalytics(
        players = listOf(player(ME, myElo), player(opponent, null)),
        uploadTime = uploadTime,
        format = "gen9vgc",
        rating = null,
        version = "1",
        winner = winner?.let(::UserName),
        url = null,
        reference = reference,
        nextBattleRef = null,
        notes = null,
    )

    private fun player(name: String, elo: Int?) = Player(
        name = UserName(name),
        teamPreview = TeamPreview(emptyList()),
        selection = listOf(PokemonName("pikachu")),
        beforeElo = elo,
        afterElo = null,
        terastallization = null,
        megaEvolution = null,
        ots = null,
        movesUsage = emptyMap(),
    )

    private companion object {
        const val ME = "me"
    }
}
