package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.lead

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.ui.composables.PokemonStatCard
import com.tambapps.pokemon.alakastats.ui.composables.ScrollToTopIfNeeded
import com.tambapps.pokemon.alakastats.ui.composables.ScrollableRow
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
internal fun LeadAndWinRow(viewModel: LeadStatsViewModel) {
    LeadRow(
        title = "Lead And Win",
        viewModel = viewModel,
        leadStats = viewModel.leadAndWinStats,
    )
}

@Composable
internal fun MostEffectiveLeadRow(viewModel: LeadStatsViewModel) {
    LeadRow(
        title = "Most Effective Leads",
        viewModel = viewModel,
        leadStats = viewModel.mostEffectiveLeadsStats,
    )
}

@Composable
internal fun MostCommonLeadRow(viewModel: LeadStatsViewModel) {
    LeadRow(
        title = "Most Common Leads",
        viewModel = viewModel,
        leadStats = viewModel.mostCommonLeadsStats,
    )
}

@Composable
private fun LeadRow(
    viewModel: LeadStatsViewModel,
    title: String,
    leadStats: List<LeadStats>,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        val isDuo = leadStats.firstOrNull()?.lead?.size?.let { it >= 2 } == true
        val spaceWidth = if (isDuo) 200.dp else 64.dp

        val scrollState = rememberScrollState()
        // Auto-scroll animation to show the row is scrollable
        LaunchedEffect(scrollState.maxValue, viewModel.useCase.filters) {
            if (scrollState.maxValue > 0) {
                scrollState.scrollTo(scrollState.maxValue)
                kotlinx.coroutines.delay(250)
                scrollState.animateScrollTo(
                    value = 0,
                    animationSpec = tween(durationMillis = 1250)
                )
            }
        }
        ScrollableRow(
            modifier = Modifier.fillMaxWidth(),
            scrollState = scrollState,
            scrollbarThickness = 16.dp
        ) {
            if (isDuo) {
                Spacer(Modifier.width(spaceWidth * 0.5f))
            }
            leadStats.forEach { leadStat ->
                LeadCard(
                    viewModel = viewModel,
                    pokemonName = leadStat.lead.first(),
                    pokemonName2 = leadStat.lead.getOrNull(1),
                    stat = leadStat.stats,
                    modifier = Modifier.size(256.dp).padding(bottom = 32.dp)
                )
                Spacer(Modifier.width(spaceWidth))
            }
        }
    }
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
