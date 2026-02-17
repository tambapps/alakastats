package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.lead

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.GameOutput
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsContext
import com.tambapps.pokemon.alakastats.domain.model.withContext
import com.tambapps.pokemon.alakastats.domain.usecase.ManageReplayFiltersUseCase
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.TeamlyticsFiltersTabViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.TeamlyticsTabViewModel
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.collections.component1
import kotlin.collections.component2

class LeadStatsViewModel(
    override val useCase: ManageReplayFiltersUseCase,
    override val pokemonImageService: PokemonImageService,
    ): TeamlyticsFiltersTabViewModel() {

    val hasNoData: Boolean get() = leadAndWinStats.isEmpty() && mostCommonLeadsStats.isEmpty() && mostEffectiveLeadsStats.isEmpty()
    var leadAndWinStats by mutableStateOf(listOf<LeadStats>())
    var mostCommonLeadsStats by mutableStateOf(listOf<LeadStats>())
    var mostEffectiveLeadsStats by mutableStateOf(listOf<LeadStats>())

    override var isTabLoading by mutableStateOf(false)
        private set

    private val scope = CoroutineScope(Dispatchers.Default)
    private val winRateComparator: Comparator<LeadStats> = compareBy { leadStat -> - leadStat.stats.winRate }
    private val totalComparator: Comparator<LeadStats> = compareBy { leadStat -> - leadStat.stats.total }

    fun loadStats() {
        if (isTabLoading) {
            return
        }
        isTabLoading = true
        scope.launch {
            val (newLeadAndWinStats, newMostCommonLeadsStats, newMostEffectiveLeadsStats) = computeStats()
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                leadAndWinStats = newLeadAndWinStats
                mostCommonLeadsStats = newMostCommonLeadsStats
                mostEffectiveLeadsStats = newMostEffectiveLeadsStats
                isTabLoading = false
            }
        }
    }

    private fun computeStats(): Triple<List<LeadStats>, List<LeadStats>, List<LeadStats>> = useCase.filteredTeam.withContext {
        val replays = team.replays.filter { it.gameOutput != GameOutput.UNKNOWN }
        val leadsStats = computeLeadsStats(replays)
        val pokemonLeadAndWins = computeIndividualStats(replays)
        val leadAndWinStats = pokemonLeadAndWins
            .entries
            .asSequence()
            .map { (pokemon, stats) -> LeadStats(listOf(pokemon), stats) }
            .sortedWith(winRateComparator.then(totalComparator))
            .toList()
        val mostCommonLeadsStats = leadsStats.sortedWith(totalComparator.then(winRateComparator)).toList()
        val mostEffectiveLeadsStats = leadsStats.sortedWith(winRateComparator.then(totalComparator)).toList()
        return Triple(leadAndWinStats, mostCommonLeadsStats, mostEffectiveLeadsStats)
    }

    private fun TeamlyticsContext.computeLeadsStats(replays: List<ReplayAnalytics>): List<LeadStats> {
        return replays.groupBy { it.youPlayer.lead }
            .mapValues { (_, replaysByLead) -> winStats(replaysByLead) }
            .map { (pokemons, stats) -> LeadStats(pokemons, stats) }
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

data class LeadStats(
    val lead: List<PokemonName>,
    val stats: WinStats
)