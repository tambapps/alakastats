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
     * @param games the saved games, by game number
     * @param players the players, in their order of the first saved game
     * @param wins the games each player won, in the order of [players]
     * @param missingGames whether games of the series aren't saved, e.g. the first one uploaded before the loaded period
     */
    data class Series(
        val seriesRef: String,
        val bestOf: Int,
        val games: List<ShowdownReplaySummary>,
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
 * Groups the games of a same series, each series taking the place of its first game in [sortedReplays]
 *
 * @param sortedReplays the replays to show, sorted
 */
fun groupedBySeries(sortedReplays: List<ShowdownReplaySummary>): List<ShowdownReplayListItem> {
    val gamesBySeries = sortedReplays.filter { it.bestOf != null }.groupBy { it.bestOf!!.seriesRef }
    val listedSeries = mutableSetOf<String>()
    return sortedReplays.mapNotNull { replay ->
        val bestOf = replay.bestOf ?: return@mapNotNull ShowdownReplayListItem.Single(replay)
        if (!listedSeries.add(bestOf.seriesRef)) return@mapNotNull null
        val games = gamesBySeries.getValue(bestOf.seriesRef).sortedBy { it.bestOf!!.game }
        val players = games.first().players
        val playedGames = games.maxOf { it.bestOf!!.game }
        ShowdownReplayListItem.Series(
            seriesRef = bestOf.seriesRef,
            bestOf = bestOf.of,
            games = games,
            players = players,
            // by name: the players' order may change from a game to another
            wins = players.map { player -> games.count { it.winner?.matches(player.name) == true } },
            // e.g. the game 2 without the game 1
            missingGames = games.map { it.bestOf!!.game }.toSet() != (1..playedGames).toSet(),
        )
    }
}
