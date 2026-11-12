package com.tambapps.pokemon.alakastats.ui.model

import com.tambapps.pokemon.alakastats.domain.model.BestOf
import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplayPlayer
import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplaySummary
import com.tambapps.pokemon.alakastats.domain.model.UserName
import com.tambapps.pokemon.alakastats.domain.model.withSeriesRatings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShowdownReplaySeriesTest {

    private fun player(name: String) = ShowdownReplayPlayer(UserName(name), emptyList(), emptyList(), emptyList(), null, null)

    private fun game(
        id: String,
        winner: String?,
        bestOf: BestOf?,
        players: List<String> = listOf("alaka", "opponent"),
    ) = ShowdownReplaySummary(
        id = id,
        formatId = "gen9vgcbo3",
        uploadTime = 100,
        rating = null,
        winner = winner?.let(::UserName),
        players = players.map(::player),
        bestOf = bestOf,
    )

    @Test
    fun theEarlierGamesOfASeriesTakeTheRatingOfItsLastOne() {
        val replays = listOf(
            game("g1", "alaka", BestOf(1, 3, "s")),
            game("g2", "opponent", BestOf(2, 3, "s")),
            game("g3", "alaka", BestOf(3, 3, "s")).copy(rating = 1600),
            game("other", "alaka", BestOf(1, 3, "other")),
            game("single", "alaka", bestOf = null),
        ).withSeriesRatings()
        assertEquals(listOf(1600, 1600, 1600, null, null), replays.map { it.rating })
    }

    @Test
    fun theSeriesPlayersHaveTheEloOfTheGameTellingIt() {
        val rated = game("g3", "alaka", BestOf(3, 3, "s"), players = listOf("opponent", "alaka")).let { game ->
            game.copy(players = game.players.map { it.copy(beforeElo = if (it.name.value == "alaka") 1500 else 1400) })
        }
        val items = groupedBySeries(listOf(
            game("g1", "alaka", BestOf(1, 3, "s")),
            game("g2", "opponent", BestOf(2, 3, "s")),
            rated,
        ))
        val series = assertIs<ShowdownReplayListItem.Series>(items.single())
        // in the order of the first game, whatever the order of the last one
        assertEquals(listOf("alaka" to 1500, "opponent" to 1400), series.players.map { it.name.value to it.beforeElo })
    }

    @Test
    fun theGamesOfASeriesAreGroupedWhereItsFirstListedGameIs() {
        val single = game("single", "alaka", bestOf = null)
        // newest first
        val replays = listOf(
            game("s1-g2", "alaka", BestOf(2, 3, "s1")),
            single,
            game("s1-g1", "alaka", BestOf(1, 3, "s1")),
        )
        val items = groupedBySeries(replays)

        assertEquals(listOf("s1", "single"), items.map { it.key })
        val series = assertIs<ShowdownReplayListItem.Series>(items[0])
        assertEquals(listOf("s1-g1", "s1-g2"), series.games.map { it.id })
        assertEquals(listOf(2, 0), series.wins)
        assertEquals(UserName("alaka"), series.winner)
    }

    @Test
    fun theScoreFollowsThePlayersWhateverTheirOrderInEachGame() {
        val items = groupedBySeries(listOf(
            game("g1", "opponent", BestOf(1, 3, "s")),
            game("g2", "alaka", BestOf(2, 3, "s"), players = listOf("opponent", "alaka")),
            game("g3", "opponent", BestOf(3, 3, "s")),
        ))
        val series = assertIs<ShowdownReplayListItem.Series>(items.single())
        assertEquals(listOf("alaka", "opponent"), series.players.map { it.name.value })
        assertEquals(listOf(1, 2), series.wins)
        assertEquals(UserName("opponent"), series.winner)
    }

    @Test
    fun theScoreCountsTheGamesTheFiltersHide() {
        val all = listOf(
            game("g1", "opponent", BestOf(1, 3, "s")),
            game("g2", "alaka", BestOf(2, 3, "s")),
            game("g3", "alaka", BestOf(3, 3, "s")),
        )
        val series = assertIs<ShowdownReplayListItem.Series>(groupedBySeries(listOf(all[1]), all).single())
        assertEquals(listOf("g2"), series.games.map { it.id })
        assertEquals(2, series.hiddenGames)
        assertEquals(listOf(2, 1), series.wins)
        assertEquals(UserName("alaka"), series.winner)
    }

    @Test
    fun aSeriesMissingGamesHasNoWinner() {
        val items = groupedBySeries(listOf(
            game("g2", "alaka", BestOf(2, 3, "s")),
            game("g3", "alaka", BestOf(3, 3, "s")),
        ))
        val series = assertIs<ShowdownReplayListItem.Series>(items.single())
        assertTrue(series.missingGames)
        assertNull(series.winner)
    }

    @Test
    fun aSeriesWithoutAMajorityYetHasNoWinner() {
        val items = groupedBySeries(listOf(
            game("g1", "alaka", BestOf(1, 3, "s")),
            game("g2", "opponent", BestOf(2, 3, "s")),
        ))
        assertNull(assertIs<ShowdownReplayListItem.Series>(items.single()).winner)
    }

    @Test
    fun theOnlySavedGameOfASeriesIsShownAlone() {
        val game = game("g2", "alaka", BestOf(2, 3, "s"))
        val item = assertIs<ShowdownReplayListItem.Single>(groupedBySeries(listOf(game)).single())
        assertEquals(game, item.replay)
    }
}
