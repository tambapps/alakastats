package com.tambapps.pokemon.alakastats.ui.model

import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.BestOf
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.domain.model.GameOutcome
import com.tambapps.pokemon.alakastats.domain.model.Player
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.TeamPreview
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsData
import com.tambapps.pokemon.alakastats.domain.model.UserName
import com.tambapps.pokemon.pokepaste.parser.PokePaste
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant
import kotlin.uuid.Uuid

class ReplaySeriesTest {

    private fun team(replays: List<ReplayAnalytics>) = Teamlytics(
        id = Uuid.random(),
        name = "Team",
        pokePaste = PokePaste(emptyList()),
        replays = replays,
        sdNames = listOf(UserName("me")),
        lastUpdatedAt = Instant.fromEpochMilliseconds(0),
        data = TeamlyticsData(emptyMap()),
        notes = null,
        matchupPlans = emptyList(),
        format = Format.NONE,
    )

    private fun game(reference: String, won: Boolean, series: String? = null, game: Int = 1) = ReplayAnalytics(
        players = listOf(player("me"), player("opponent")),
        uploadTime = 0,
        format = "gen9vgc2026regmabo3",
        rating = null,
        version = "1",
        winner = UserName(if (won) "me" else "opponent"),
        url = "https://replay.pokemonshowdown.com/$reference",
        reference = reference,
        nextBattleRef = null,
        notes = null,
        bestOf = series?.let { BestOf(game, 3, it) },
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

    @Test
    fun seriesTakesThePlaceOfItsFirstListedGame() {
        val bo1 = game("bo1", won = true)
        val g1 = game("g1", won = false, series = "s", game = 1)
        val g2 = game("g2", won = true, series = "s", game = 2)
        val g3 = game("g3", won = true, series = "s", game = 3)
        val team = team(listOf(g1, g2, g3, bo1))

        val items = team.groupedBySeries(listOf(g3, bo1, g1, g2))

        assertEquals(listOf("s", "bo1"), items.map { it.key })
        val series = assertIs<ReplayListItem.Series>(items[0])
        assertEquals(listOf(g1, g2, g3), series.games)
        assertEquals(2, series.wins)
        assertEquals(1, series.losses)
        assertEquals(GameOutcome.WIN, series.outcome)
    }

    @Test
    fun filtersHideGamesButNotTheScore() {
        val g1 = game("g1", won = false, series = "s", game = 1)
        val g2 = game("g2", won = false, series = "s", game = 2)
        val team = team(listOf(g1, g2))

        val series = assertIs<ReplayListItem.Series>(team.groupedBySeries(listOf(g2)).single())

        assertEquals(listOf(g2), series.games)
        assertEquals(1, series.hiddenGames)
        assertEquals(0, series.wins)
        assertEquals(2, series.losses)
        assertEquals(GameOutcome.LOOSE, series.outcome)
    }

    @Test
    fun seriesWithMissingGamesHasNoOutcome() {
        // the game 2 wasn't added
        val g1 = game("g1", won = true, series = "s", game = 1)
        val g3 = game("g3", won = true, series = "s", game = 3)
        val team = team(listOf(g1, g3))

        val series = assertIs<ReplayListItem.Series>(team.groupedBySeries(listOf(g1, g3)).single())

        assertEquals(true, series.missingGames)
        assertEquals(GameOutcome.UNKNOWN, series.outcome)
    }

    @Test
    fun unfinishedSeriesHasNoOutcome() {
        val g1 = game("g1", won = true, series = "s", game = 1)
        val g2 = game("g2", won = false, series = "s", game = 2)
        val team = team(listOf(g1, g2))

        val series = assertIs<ReplayListItem.Series>(team.groupedBySeries(listOf(g1, g2)).single())

        assertEquals(false, series.missingGames)
        assertEquals(GameOutcome.UNKNOWN, series.outcome)
    }

    @Test
    fun bo3ReplaysWithoutTheirGameNumberAreToReload() {
        val old = game("old", won = true)
        val numbered = game("numbered", won = true, series = "s")
        val manual = game("manual", won = true).copy(url = null)

        assertEquals(listOf(old), team(listOf(old, numbered, manual)).ungroupedSeriesReplays)
    }
}
