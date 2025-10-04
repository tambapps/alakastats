package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.lead

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.tambapps.pokemon.alakastats.domain.model.GameOutput
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsContext
import com.tambapps.pokemon.alakastats.domain.model.withContext
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LeadStatsViewModel(
    val team: Teamlytics,
    val pokemonImageService: PokemonImageService,
    ) {
    val duoStatsMap: SnapshotStateMap<List<String>, WinStats> = mutableStateMapOf()
    val pokemonStats: SnapshotStateMap<String, WinStats> = mutableStateMapOf()

    private val scope = CoroutineScope(Dispatchers.Default)

    fun loadStats() = scope.launch {
        team.withContext {
            val replays = team.replays.filter { it.gameOutput != GameOutput.UNKNOWN }
            loadDuoStats(replays)
            loadIndividualStats(replays)
        }
    }

    private fun TeamlyticsContext.loadDuoStats(replays: List<ReplayAnalytics>) {
        val result = replays.groupBy { it.youPlayer.lead }
            .mapValues { (_, replaysByLead) -> winStats(replaysByLead) }

        duoStatsMap.clear()
        duoStatsMap.putAll(result)
    }

    private fun TeamlyticsContext.loadIndividualStats(replays: List<ReplayAnalytics>) {
        val leadPokemons = replays.asSequence().flatMap { it.youPlayer.lead }.toSet()
        val result = leadPokemons.associateWith { leadPokemon ->
            val leadPokemonReplays = replays.filter { replay -> replay.youPlayer.lead.contains(leadPokemon) }
            winStats(leadPokemonReplays)
        }
        pokemonStats.clear()
        pokemonStats.putAll(result)
    }
}

fun TeamlyticsContext.winStats(replays: List<ReplayAnalytics>): WinStats {
    val winCount = replays.count { it.gameOutput == GameOutput.WIN }
    val total = replays.size
    return WinStats(
        winCount = winCount,
        total = total,
        winRate = if (total != 0) winCount.toFloat() / total.toFloat() else 0f
    )
}

class WinStats(
    val winCount: Int,
    val total: Int,
    val winRate: Float
)