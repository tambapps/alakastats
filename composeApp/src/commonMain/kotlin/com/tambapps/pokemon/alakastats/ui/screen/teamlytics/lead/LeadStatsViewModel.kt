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
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.TeamlyticsTabViewModel
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LeadStatsViewModel(
    override val useCase: ManageReplayFiltersUseCase,
    val pokemonImageService: PokemonImageService,
    ): TeamlyticsTabViewModel() {

    val hasNoData: Boolean get() = leadAndWinStats.isEmpty() && mostCommonLeadStats.isEmpty() && mostEffectiveLeadStats.isEmpty()
    var leadAndWinStats by mutableStateOf(listOf<LeadStat>())
    var mostCommonLeadStats by mutableStateOf(listOf<LeadStat>())
    var mostEffectiveLeadStats by mutableStateOf(listOf<LeadStat>())


    override var isTabLoading by mutableStateOf(false)
        private set

    private val scope = CoroutineScope(Dispatchers.Default)

    private val winRateComparator: Comparator<LeadStat> = compareBy { leadStat -> - leadStat.stats.winRate }
    private val totalComparator: Comparator<LeadStat> = compareBy { leadStat -> - leadStat.stats.total }

    fun loadStats() {
        if (isTabLoading) {
            return
        }
        isTabLoading = true
        scope.launch {
            val (duoStats, individualStats) = useCase.filteredTeam.withContext {
                val replays = team.replays.filter { it.gameOutput != GameOutput.UNKNOWN }
                val duoStats = computeDuoStats(replays)
                val pokemonLeadAndWins = computeIndividualStats(replays)
                duoStats to pokemonLeadAndWins
            }
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                leadAndWinStats = individualStats
                    .entries
                    .asSequence()
                    .map { (pokemon, stats) -> LeadStat(listOf(pokemon), stats) }
                    .sortedWith(winRateComparator.then(totalComparator))
                    .toList()

                val leadsSequence = duoStats
                    .entries
                    .asSequence()
                    .map { (pokemons, stats) -> LeadStat(pokemons, stats) }
                mostCommonLeadStats = leadsSequence.sortedWith(totalComparator.then(winRateComparator)).toList()
                mostEffectiveLeadStats = leadsSequence.sortedWith(winRateComparator.then(totalComparator)).toList()
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

data class LeadStat(
    val lead: List<PokemonName>,
    val stats: WinStats
)