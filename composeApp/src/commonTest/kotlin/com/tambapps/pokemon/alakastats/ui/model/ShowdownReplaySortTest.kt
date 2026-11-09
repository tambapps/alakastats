package com.tambapps.pokemon.alakastats.ui.model

import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplaySummary
import kotlin.test.Test
import kotlin.test.assertEquals

class ShowdownReplaySortTest {

    private fun replay(id: String, uploadTime: Long, rating: Int?) =
        ShowdownReplaySummary(id, "gen9vgc", uploadTime, rating, winner = null, players = emptyList(), bestOf = null)

    private val replays = listOf(
        replay("old-high", 100, 1800),
        replay("new-low", 300, 1300),
        replay("unrated", 200, null),
        replay("new-high", 400, 1800),
    )

    private fun ids(sort: ShowdownReplaySort) = replays.sortedBy(sort).map { it.id }

    @Test
    fun byDefaultTheNewestComeFirst() {
        assertEquals(listOf("new-high", "new-low", "unrated", "old-high"), ids(ShowdownReplaySort()))
        assertEquals(listOf("old-high", "unrated", "new-low", "new-high"), ids(ShowdownReplaySort(descending = false)))
    }

    @Test
    fun byEloTheUnratedComeLastAndTheSameEloTheNewestFirst() {
        assertEquals(
            listOf("new-high", "old-high", "new-low", "unrated"),
            ids(ShowdownReplaySort(ShowdownReplaySortCriterion.RATING)),
        )
        assertEquals(
            listOf("new-low", "new-high", "old-high", "unrated"),
            ids(ShowdownReplaySort(ShowdownReplaySortCriterion.RATING, descending = false)),
        )
    }
}
