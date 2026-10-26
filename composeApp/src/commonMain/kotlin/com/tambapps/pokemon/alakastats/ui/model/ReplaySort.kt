package com.tambapps.pokemon.alakastats.ui.model

import com.tambapps.pokemon.alakastats.domain.model.GameOutcome
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.withContext

enum class ReplaySortCriterion(
    val label: String,
    val descendingLabel: String,
    val ascendingLabel: String,
    val defaultDescending: Boolean,
) {
    DATE("Date", "Newest first", "Oldest first", defaultDescending = true),
    OUTCOME("Outcome", "Wins first", "Losses first", defaultDescending = true),
    OPPONENT("Opponent", "Z → A", "A → Z", defaultDescending = false),
    ELO("Your Elo", "Highest first", "Lowest first", defaultDescending = true),
    OPPONENT_ELO("Opp. Elo", "Highest first", "Lowest first", defaultDescending = true),
    // the order the replays are stored in, which is the order they were added in
    ADDED("Added", "Last added first", "First added first", defaultDescending = true),
}

/**
 * The order the replays are displayed in. It only changes the display: the stats and the team's data stay the same
 */
data class ReplaySort(
    val criterion: ReplaySortCriterion = ReplaySortCriterion.DATE,
    val descending: Boolean = criterion.defaultDescending,
) {
    val directionLabel get() = if (descending) criterion.descendingLabel else criterion.ascendingLabel

    val isDefault get() = this == ReplaySort()
}

private typealias IndexedReplay = IndexedValue<ReplayAnalytics>

/**
 * Sorts the replays of the team. The replays missing the value sorted by (no date, no Elo, unknown outcome) are
 * always last, whatever the direction. Ties are ordered from the newest replay, then from the last added one
 */
fun Teamlytics.sortedReplays(sort: ReplaySort): List<ReplayAnalytics> = withContext {
    val descending = sort.descending
    val comparator: Comparator<IndexedReplay> = when (sort.criterion) {
        // the same dates are ordered by when they were added, so that the ascending order is the descending one reversed
        ReplaySortCriterion.DATE -> keyComparator(descending) { it.value.date }
            .then(keyComparator(descending) { it.index })
        ReplaySortCriterion.OUTCOME -> keyComparator(descending) {
            when (it.value.gameOutcome) {
                GameOutcome.WIN -> 1
                GameOutcome.LOOSE -> 0
                GameOutcome.UNKNOWN -> null
            }
        }.then(NEWEST_FIRST)
        ReplaySortCriterion.OPPONENT -> keyComparator(descending) { it.value.opponentPlayer.name.value.lowercase() }
            .then(NEWEST_FIRST)
        ReplaySortCriterion.ELO -> keyComparator(descending) { it.value.youPlayer.beforeElo }
            .then(NEWEST_FIRST)
        ReplaySortCriterion.OPPONENT_ELO -> keyComparator(descending) { it.value.opponentPlayer.beforeElo }
            .then(NEWEST_FIRST)
        ReplaySortCriterion.ADDED -> keyComparator(descending) { it.index }
    }
    replays.withIndex().sortedWith(comparator).map { it.value }
}

// a replay without upload time has 0
private val ReplayAnalytics.date get() = uploadTime.takeIf { it > 0 }

private val NEWEST_FIRST: Comparator<IndexedReplay> =
    keyComparator(descending = true) { it.value.date }
        .then(keyComparator(descending = true) { it.index })

// the missing keys are last in both directions
private fun <T : Comparable<T>> keyComparator(descending: Boolean, key: (IndexedReplay) -> T?) =
    Comparator<IndexedReplay> { a, b ->
        val keyA = key(a)
        val keyB = key(b)
        when {
            keyA == null && keyB == null -> 0
            keyA == null -> 1
            keyB == null -> -1
            descending -> keyB.compareTo(keyA)
            else -> keyA.compareTo(keyB)
        }
    }
