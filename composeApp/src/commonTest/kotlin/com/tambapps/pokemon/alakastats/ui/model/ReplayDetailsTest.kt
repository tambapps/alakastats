package com.tambapps.pokemon.alakastats.ui.model

import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.BestOf
import com.tambapps.pokemon.alakastats.domain.model.GameEndReason
import com.tambapps.pokemon.alakastats.domain.model.Player
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.TeamPreview
import com.tambapps.pokemon.alakastats.domain.model.UserName
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
    fun gameToTheEndTellsTheScoreFromTheTeamSide() {
        val replay = replay(me = player("me", fainted = 2), opponent = player("opp", fainted = 4))

        assertEquals("2–0 · 9 turns · Oct 12", details(replay))
    }

    @Test
    fun forfeitTellsWhatWasLeft() {
        val replay = replay(
            me = player("me", fainted = 1),
            opponent = player("opp", fainted = 3),
            endReason = GameEndReason.FORFEIT,
        )

        assertEquals("3–1 · Forfeit · 9 turns · Oct 12", details(replay))
    }

    @Test
    fun noScoreWhenTheTeamPlayerIsUnknown() {
        val replay = replay(me = player("me", fainted = 0), opponent = player("opp", fainted = 4))

        assertEquals("9 turns · Oct 12", details(replay, showScore = false))
    }

    @Test
    fun replayLoadedBeforeTheScoreWasKnownOnlyTellsItsDate() {
        val replay = replay(me = player("me", fainted = 0), opponent = player("opp", fainted = 0), turns = null, pickCount = null)

        assertEquals("Oct 12", details(replay))
    }

    @Test
    fun gameOfASeriesLeavesTheDateAndTheSeriesToItsCard() {
        val replay = replay(me = player("me", fainted = 4), opponent = player("opp", fainted = 3), bestOf = BestOf(2, 3, "s"))

        assertEquals("Game 2 of 3", details(replay).substringAfterLast(" · "))
        assertEquals("0–1 · 9 turns", details(replay, inSeries = true))
    }

    @Test
    fun dateOfAnotherYearHasItsYear() {
        val lastYear = LocalDateTime(2025, 3, 5, 20, 0).toInstant(utc).epochSeconds

        assertEquals("Mar 5, 2025", formatReplayDate(lastYear, utc, now))
        assertNull(formatReplayDate(0, utc, now))
    }

    private fun details(replay: ReplayAnalytics, showScore: Boolean = true, inSeries: Boolean = false) =
        replayDetails(replay, replay.players[0], replay.players[1], showScore, inSeries, utc, now)

    private fun replay(
        me: Player,
        opponent: Player,
        turns: Int? = 9,
        pickCount: Int? = 4,
        endReason: GameEndReason? = null,
        bestOf: BestOf? = null,
    ) = ReplayAnalytics(
        players = listOf(me, opponent),
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
        pickCount = pickCount,
        endReason = endReason,
    )

    private fun player(name: String, fainted: Int) = Player(
        name = UserName(name),
        teamPreview = TeamPreview(emptyList()),
        selection = emptyList(),
        beforeElo = null,
        afterElo = null,
        terastallization = null,
        megaEvolution = null,
        ots = null,
        movesUsage = emptyMap(),
        fainted = (1..fainted).map { PokemonName("pokemon$it") },
    )
}
