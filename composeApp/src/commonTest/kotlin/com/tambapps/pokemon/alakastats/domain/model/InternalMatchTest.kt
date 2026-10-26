package com.tambapps.pokemon.alakastats.domain.model

import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.pokepaste.parser.PokePaste
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

class InternalMatchTest {

    private val team = Teamlytics(
        id = Uuid.random(),
        name = "Team",
        pokePaste = PokePaste(emptyList()),
        replays = emptyList(),
        sdNames = listOf(UserName("main"), UserName("alt")),
        lastUpdatedAt = Instant.fromEpochMilliseconds(0),
        data = TeamlyticsData(emptyMap()),
        notes = null,
        matchupPlans = emptyList(),
        format = Format.NONE,
    )

    @Test
    fun matchBetweenTwoSdNamesIsInternal() {
        assertTrue(team.isInternalMatch(replay("main", "alt", winner = "alt")))
        assertFalse(team.isInternalMatch(replay("main", "stranger", winner = "main")))
    }

    @Test
    fun internalMatchOutcomeIsTheFirstPlayers() {
        assertEquals(GameOutcome.LOOSE, team.getGameOutcome(replay("main", "alt", winner = "alt")))
        assertEquals(GameOutcome.WIN, team.getGameOutcome(replay("alt", "main", winner = "alt")))
    }

    @Test
    fun internalMatchDoesNotCountInTheStats() {
        team.withContext {
            assertFalse(replay("main", "alt", winner = "alt").countsInStats)
            assertTrue(replay("main", "stranger", winner = "stranger").countsInStats)
            assertFalse(replay("stranger", "other", winner = "other").countsInStats)
        }
    }

    @Test
    fun internalMatchDoesNotCountInTheWinRate() {
        val replays = listOf(
            replay("main", "stranger", winner = "main"),
            replay("alt", "stranger", winner = "stranger"),
            replay("main", "alt", winner = "main"),
        )
        assertEquals(50, computeWinRatePercentage(team.sdNames, replays))
        assertEquals(0, computeWinRatePercentage(team.sdNames, replays.takeLast(1)))
    }

    private fun replay(player1: String, player2: String, winner: String) = ReplayAnalytics(
        players = listOf(player(player1), player(player2)),
        uploadTime = 0,
        format = "gen9vgc",
        rating = null,
        version = "1",
        winner = UserName(winner),
        url = null,
        reference = "$player1-$player2",
        nextBattleRef = null,
        notes = null,
    )

    private fun player(name: String) = Player(
        name = UserName(name),
        teamPreview = TeamPreview(emptyList()),
        selection = listOf(PokemonName("pikachu")),
        beforeElo = null,
        afterElo = null,
        terastallization = null,
        megaEvolution = null,
        ots = null,
        movesUsage = emptyMap(),
    )
}
