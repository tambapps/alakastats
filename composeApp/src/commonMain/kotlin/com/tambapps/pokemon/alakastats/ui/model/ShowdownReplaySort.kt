package com.tambapps.pokemon.alakastats.ui.model

import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplaySummary

enum class ShowdownReplaySortCriterion(
    override val label: String,
    override val descendingLabel: String,
    override val ascendingLabel: String,
    override val defaultDescending: Boolean,
) : SortCriterion {
    DATE("Date", "Newest first", "Oldest first", defaultDescending = true),
    RATING("Elo", "Highest first", "Lowest first", defaultDescending = true),
}

data class ShowdownReplaySort(
    val criterion: ShowdownReplaySortCriterion = ShowdownReplaySortCriterion.DATE,
    val descending: Boolean = criterion.defaultDescending,
) {
    val isDefault get() = this == ShowdownReplaySort()
}

/**
 * The replays in the sort's order. The replays without rating come last when sorting by rating, the newest first
 */
fun List<ShowdownReplaySummary>.sortedBy(sort: ShowdownReplaySort): List<ShowdownReplaySummary> {
    val byDate = compareBy<ShowdownReplaySummary> { it.uploadTime }
    val comparator = when (sort.criterion) {
        ShowdownReplaySortCriterion.DATE -> if (sort.descending) byDate.reversed() else byDate
        ShowdownReplaySortCriterion.RATING -> Comparator<ShowdownReplaySummary> { a, b ->
            val ratingA = a.rating
            val ratingB = b.rating
            when {
                ratingA == null && ratingB == null -> 0
                ratingA == null -> 1
                ratingB == null -> -1
                sort.descending -> ratingB.compareTo(ratingA)
                else -> ratingA.compareTo(ratingB)
            }
        }.then(byDate.reversed())
    }
    return sortedWith(comparator)
}
