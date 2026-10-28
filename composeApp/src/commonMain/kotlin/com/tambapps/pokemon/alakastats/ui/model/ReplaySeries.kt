package com.tambapps.pokemon.alakastats.ui.model

import com.tambapps.pokemon.alakastats.domain.model.GameOutcome
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.getGameOutcome

/**
 * What the replays tab lists: a replay, or the games of a series (e.g. a best-of-3) grouped together
 */
sealed interface ReplayListItem {
    /** unique in the list, e.g. for the lazy list keys */
    val key: String

    data class Single(val replay: ReplayAnalytics) : ReplayListItem {
        override val key get() = replay.reference
    }

    /**
     * @param games the games the filters show, by game number
     * @param hiddenGames the number of the series' games the filters hide
     * @param wins the games of the whole series the user won, the filters don't change the score
     * @param missingGames whether games of the series weren't added to the team
     */
    data class Series(
        val seriesRef: String,
        val bestOf: Int,
        val games: List<ReplayAnalytics>,
        val hiddenGames: Int,
        val wins: Int,
        val losses: Int,
        val missingGames: Boolean,
    ) : ReplayListItem {
        override val key get() = seriesRef

        /** UNKNOWN until a player won the majority of the games, or while games are missing */
        val outcome: GameOutcome get() = when {
            missingGames -> GameOutcome.UNKNOWN
            wins > bestOf / 2 -> GameOutcome.WIN
            losses > bestOf / 2 -> GameOutcome.LOOSE
            else -> GameOutcome.UNKNOWN
        }
    }
}

/**
 * Groups the games of a same series, each series taking the place of its first game in [sortedReplays]
 *
 * @param sortedReplays the replays to show, filtered and sorted
 */
fun Teamlytics.groupedBySeries(sortedReplays: List<ReplayAnalytics>): List<ReplayListItem> {
    val shownGames = sortedReplays.filter { it.bestOf != null }.groupBy { it.bestOf!!.seriesRef }
    val allGames = replays.filter { it.bestOf != null }.groupBy { it.bestOf!!.seriesRef }
    val listedSeries = mutableSetOf<String>()
    return sortedReplays.mapNotNull { replay ->
        val bestOf = replay.bestOf ?: return@mapNotNull ReplayListItem.Single(replay)
        if (!listedSeries.add(bestOf.seriesRef)) return@mapNotNull null
        val games = shownGames.getValue(bestOf.seriesRef).sortedBy { it.bestOf!!.game }
        val seriesGames = allGames[bestOf.seriesRef] ?: games
        val outcomes = seriesGames.map { getGameOutcome(it) }
        val playedGames = seriesGames.maxOf { it.bestOf!!.game }
        ReplayListItem.Series(
            seriesRef = bestOf.seriesRef,
            bestOf = bestOf.of,
            games = games,
            hiddenGames = seriesGames.size - games.size,
            wins = outcomes.count { it == GameOutcome.WIN },
            losses = outcomes.count { it == GameOutcome.LOOSE },
            // e.g. the game 3 without the game 2
            missingGames = seriesGames.map { it.bestOf!!.game }.toSet() != (1..playedGames).toSet(),
        )
    }
}

/**
 * The best-of-3 replays loaded before the app knew their game number: they can't be grouped until reloaded
 */
val Teamlytics.ungroupedSeriesReplays: List<ReplayAnalytics>
    get() = replays.filter { it.bestOf == null && it.showdownUrl != null && it.format.lowercase().endsWith("bo3") }
