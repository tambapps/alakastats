package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.lead

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.tambapps.pokemon.alakastats.domain.model.GameOutput
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.getGameOutput
import com.tambapps.pokemon.alakastats.domain.model.getOpponentPlayer
import com.tambapps.pokemon.alakastats.domain.model.getPlayers
import com.tambapps.pokemon.alakastats.domain.model.getYouPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LeadStatsViewModel(
    val team: Teamlytics,
    ) {
    val duoStatsMap: SnapshotStateMap<List<String>, WinStats> = mutableStateMapOf()
    val pokemonStats: SnapshotStateMap<String, WinStats> = mutableStateMapOf()

    private val scope = CoroutineScope(Dispatchers.Default)


    fun loadStats() = scope.launch {
        val replays = team.replays.filter { team.getGameOutput(it) != GameOutput.UNKNOWN }

        val duoStatsMap = replays.groupBy { team.getYouPlayer(it).lead }
            .mapValues { (_, replaysByLead) ->
            val winCount = replaysByLead.count { it -> team.getGameOutput(it) == GameOutput.WIN }
            val total = replaysByLead.size
            WinStats(
                winCount = winCount,
                total = total,
                winRate = if (total != 0) winCount.toFloat() / total.toFloat() else 0f
            )
        }
        this@LeadStatsViewModel.duoStatsMap.apply {
            clear()
            putAll(duoStatsMap)
        }

        val leadPokemons = replays.flatMap { team.getYouPlayer(it).lead }



    }



}

class WinStats(
    val winCount: Int,
    val total: Int,
    val winRate: Float
) {
}