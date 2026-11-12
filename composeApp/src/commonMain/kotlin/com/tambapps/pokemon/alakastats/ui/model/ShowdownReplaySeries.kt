package com.tambapps.pokemon.alakastats.ui.model

import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplayPlayer
import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplaySummary
import com.tambapps.pokemon.alakastats.domain.model.UserName

/**
 * What the Showdown replays screen lists: a replay, or the saved games of a series (e.g. a best-of-3) grouped together
 */
sealed interface ShowdownReplayListItem {
    /** unique in the list, e.g. for the lazy list keys */
    val key: String

    data class Single(val replay: ShowdownReplaySummary) : ShowdownReplayListItem {
        override val key get() = replay.id
    }

    /**
     * @param games the saved games the filters show, by game number
     * @param hiddenGames the number of the series' saved games the filters hide
     * @param players the players, in their order of the first saved game
     * @param wins the saved games each player won, in the order of [players], the filters don't change the score
     * @param missingGames whether games of the series aren't saved, e.g. the first one uploaded before the loaded period
     */
    data class Series(
        val seriesRef: String,
        val bestOf: Int,
        val games: List<ShowdownReplaySummary>,
        val hiddenGames: Int,
        val players: List<ShowdownReplayPlayer>,
        val wins: List<Int>,
        val missingGames: Boolean,
    ) : ShowdownReplayListItem {
        override val key get() = seriesRef

        /** null until a player won the majority of the games, or while games are missing */
        val winner: UserName? get() =
            if (missingGames) null
            else players.zip(wins).firstOrNull { (_, wins) -> wins > bestOf / 2 }?.first?.name
    }
}

/**
 * Groups the games of a same series, each series taking the place of its first game in [sortedReplays]. A series with
 * a single saved game is that game alone
 *
 * @param sortedReplays the replays to show, filtered and sorted
 * @param allReplays all the saved replays, for the score of the whole series
 */
fun groupedBySeries(
    sortedReplays: List<ShowdownReplaySummary>,
    allReplays: List<ShowdownReplaySummary> = sortedReplays,
): List<ShowdownReplayListItem> {
    val shownGames = sortedReplays.filter { it.bestOf != null }.groupBy { it.bestOf!!.seriesRef }
    val allGames = allReplays.filter { it.bestOf != null }.groupBy { it.bestOf!!.seriesRef }
    val listedSeries = mutableSetOf<String>()
    return sortedReplays.mapNotNull { replay ->
        val bestOf = replay.bestOf ?: return@mapNotNull ShowdownReplayListItem.Single(replay)
        if (!listedSeries.add(bestOf.seriesRef)) return@mapNotNull null
        val games = shownGames.getValue(bestOf.seriesRef).sortedBy { it.bestOf!!.game }
        val seriesGames = (allGames[bestOf.seriesRef] ?: games).sortedBy { it.bestOf!!.game }
        // the only saved game of its series: a series' card would only add a click to it
        if (seriesGames.size == 1) return@mapNotNull ShowdownReplayListItem.Single(replay)
        val players = seriesGames.first().players
        val playedGames = seriesGames.maxOf { it.bestOf!!.game }
        ShowdownReplayListItem.Series(
            seriesRef = bestOf.seriesRef,
            bestOf = bestOf.of,
            games = games,
            hiddenGames = seriesGames.size - games.size,
            players = players,
            // by name: the players' order may change from a game to another
            wins = players.map { player -> seriesGames.count { it.winner?.matches(player.name) == true } },
            // e.g. the game 2 without the game 1
            missingGames = seriesGames.map { it.bestOf!!.game }.toSet() != (1..playedGames).toSet(),
        )
    }
}
