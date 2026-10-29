package com.tambapps.pokemon.alakastats.ui.model

import com.tambapps.pokemon.alakastats.domain.model.BestOf
import com.tambapps.pokemon.alakastats.domain.model.GameEndReason
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReplayDetailsTest {

    private val utc = TimeZone.UTC
    private val now = LocalDateTime(2026, 10, 28, 12, 0).toInstant(utc)
    private val oct12 = LocalDateTime(2026, 10, 12, 20, 0).toInstant(utc).epochSeconds

    @Test
    fun gameToTheEndTellsItsLengthAndDate() {
        assertEquals("9 turns · Oct 12", details(replay()))
    }

    @Test
    fun forfeitIsTold() {
        assertEquals("Forfeit · 9 turns · Oct 12", details(replay(endReason = GameEndReason.FORFEIT)))
        assertEquals("Timeout · 9 turns · Oct 12", details(replay(endReason = GameEndReason.TIMER)))
    }

    @Test
    fun replayLoadedBeforeTheTurnsWereKnownOnlyTellsItsDate() {
        assertEquals("Oct 12", details(replay(turns = null)))
    }

    @Test
    fun gameOfASeriesLeavesTheDateAndTheSeriesToItsCard() {
        val replay = replay(bestOf = BestOf(2, 3, "s"))

        assertEquals("9 turns · Oct 12 · Game 2 of 3", details(replay))
        assertEquals("9 turns", details(replay, inSeries = true))
    }

    @Test
    fun dateOfAnotherYearHasItsYear() {
        val lastYear = LocalDateTime(2025, 3, 5, 20, 0).toInstant(utc).epochSeconds

        assertEquals("Mar 5, 2025", formatReplayDate(lastYear, utc, now))
        assertNull(formatReplayDate(0, utc, now))
    }

    private fun details(replay: ReplayAnalytics, inSeries: Boolean = false) =
        replayDetails(replay, inSeries, utc, now)

    private fun replay(
        turns: Int? = 9,
        endReason: GameEndReason? = null,
        bestOf: BestOf? = null,
    ) = ReplayAnalytics(
        players = emptyList(),
        uploadTime = oct12,
        format = "gen9vgc",
        rating = null,
        version = "1",
        winner = null,
        url = null,
        reference = "r",
        nextBattleRef = null,
        notes = null,
        bestOf = bestOf,
        turns = turns,
        endReason = endReason,
    )
}
