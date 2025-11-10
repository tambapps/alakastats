package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.lead

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.GameOutput
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsContext
import com.tambapps.pokemon.alakastats.domain.model.withContext
import com.tambapps.pokemon.alakastats.domain.usecase.ManageReplayFiltersUseCase
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.TeamlyticsTabViewModel
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LeadStatsViewModel(
    override val useCase: ManageReplayFiltersUseCase,
    val pokemonImageService: PokemonImageService,
    ): TeamlyticsTabViewModel() {
    val duoStatsMap: SnapshotStateMap<List<PokemonName>, WinStats> = mutableStateMapOf()
    val pokemonStats: SnapshotStateMap<PokemonName, WinStats> = mutableStateMapOf()

    override var isTabLoading by mutableStateOf(false)
        private set

    private val scope = CoroutineScope(Dispatchers.Default)

    fun loadStats() {
        if (isTabLoading) {
            return
        }
        isTabLoading = true
        pokemonStats.clear()
        duoStatsMap.clear()
        scope.launch {
            val (duoStats, individualStats) = useCase.filteredTeam.withContext {
                val replays = team.replays.filter { it.gameOutput != GameOutput.UNKNOWN }
                val duoStats = computeDuoStats(replays)
                val individualStats = computeIndividualStats(replays)
                duoStats to individualStats
            }
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                duoStatsMap.putAll(duoStats)
                pokemonStats.putAll(individualStats)
                isTabLoading = false
            }
        }
    }

    private fun TeamlyticsContext.computeDuoStats(replays: List<ReplayAnalytics>): Map<List<PokemonName>, WinStats> {
        return replays.groupBy { it.youPlayer.lead }
            .mapValues { (_, replaysByLead) -> winStats(replaysByLead) }
    }

    private fun TeamlyticsContext.computeIndividualStats(replays: List<ReplayAnalytics>): Map<PokemonName, WinStats> {
        val leadPokemons = replays.asSequence().flatMap { it.youPlayer.lead }.toSet()
        return leadPokemons.associateWith { leadPokemon ->
            val leadPokemonReplays = replays.filter { replay -> replay.youPlayer.isLead(leadPokemon) }
            winStats(leadPokemonReplays)
        }
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

data class WinStats(
    val winCount: Int,
    val total: Int,
    val winRate: Float
)