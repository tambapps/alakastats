package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.lead

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.ui.composables.PokemonStatCard
import com.tambapps.pokemon.alakastats.ui.composables.ScrollToTopIfNeeded
import com.tambapps.pokemon.alakastats.ui.composables.PokemonStatsRow
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.replay.NoReplay
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact

@Composable
fun LeadStatsTab(viewModel: LeadStatsViewModel) {
    val isCompact = LocalIsCompact.current
    LaunchedEffect(viewModel.useCase.filters) {
        viewModel.loadStats()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        val scrollState = rememberScrollState()
        if (!viewModel.isLoading && viewModel.hasNoData) {
            NoReplay(viewModel)
        } else if (isCompact) {
            LeadStatsTabMobile(viewModel, scrollState)
        } else {
            LeadStatsTabDesktop(viewModel, scrollState)
        }
        ScrollToTopIfNeeded(viewModel, scrollState)
    }
}

@Composable
internal fun LeadAndWinRow(viewModel: LeadStatsViewModel) =
    PokemonStatsRow(
        title = "Lead And Win",
        viewModel = viewModel,
        stats = viewModel.leadAndWinStats,
        isDuo = false
    ) { leadStat ->
        LeadCard(
            viewModel = viewModel,
            pokemonName = leadStat.lead.first(),
            pokemonName2 = leadStat.lead.getOrNull(1),
            stat = leadStat.stats,
            modifier = Modifier.size(256.dp).padding(bottom = 32.dp)
        )
    }

@Composable
internal fun MostEffectiveLeadRow(viewModel: LeadStatsViewModel) =
    PokemonStatsRow(
        title = "Most Effective Leads",
        viewModel = viewModel,
        stats = viewModel.mostEffectiveLeadsStats,
        isDuo = viewModel.mostEffectiveLeadsStats.isDuo
    ) { leadStat ->
        LeadCard(
            viewModel = viewModel,
            pokemonName = leadStat.lead.first(),
            pokemonName2 = leadStat.lead.getOrNull(1),
            stat = leadStat.stats,
            modifier = Modifier.size(256.dp).padding(bottom = 32.dp)
        )
    }

@Composable
internal fun MostCommonLeadRow(viewModel: LeadStatsViewModel) =
    PokemonStatsRow(
        title = "Most Common Leads",
        viewModel = viewModel,
        stats = viewModel.mostCommonLeadsStats,
        isDuo = viewModel.mostCommonLeadsStats.isDuo
    ) { leadStat ->
        LeadCard(
            viewModel = viewModel,
            pokemonName = leadStat.lead.first(),
            pokemonName2 = leadStat.lead.getOrNull(1),
            stat = leadStat.stats,
            modifier = Modifier.size(256.dp).padding(bottom = 32.dp)
        )
    }

@Composable
private fun LeadCard(
    viewModel: LeadStatsViewModel,
    pokemonName: PokemonName,
    pokemonName2: PokemonName? = null,
    stat: WinStats,
    modifier: Modifier
) {
    val winCount = stat.winCount
    val total = stat.total
    val text = when {
        total == 0 -> "Did not participate\nto any game"
        winCount == 0 -> "Lost all $total games"
        winCount == total && total == 1 -> "Won\n1 out of 1\ngame"
        winCount == total -> "Won all\n$total games"
        else -> "Won ${winCount}\nout of $total\ngames"
    }
    PokemonStatCard(
        pokemonImageService = viewModel.pokemonImageService,
        title = "${stat.winRate.times(100).toInt()}%",
        text = text,
        pokemonName = pokemonName,
        pokemonName2 = pokemonName2,
        modifier = modifier
    )
}

private val List<LeadStats>.isDuo get() = firstOrNull()?.lead?.size?.let { it > 1 } == true
