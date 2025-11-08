package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.lead

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.ui.composables.StatCard
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.theme.statCardPercentageWidth
import com.tambapps.pokemon.alakastats.ui.theme.statCardPokemonSpriteSize

@Composable
fun LeadStatsTab(viewModel: LeadStatsViewModel) {
    val isCompact = LocalIsCompact.current
    LaunchedEffect(Unit) {
        viewModel.loadStats()
    }
    if (!viewModel.isLoading && viewModel.duoStatsMap.isEmpty() && viewModel.pokemonStats.isEmpty()) {
        NoData()
    } else if (isCompact) {
        LeadStatsTabMobile(viewModel)
    } else {
        LeadStatsTabDesktop(viewModel)
    }
}

@Composable
private fun NoData() {
    Box(Modifier.fillMaxSize()) {
        Text("No data", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
internal fun MostCommonLeadCard(viewModel: LeadStatsViewModel, modifier: Modifier = Modifier) {
    LeadCard(
        viewModel = viewModel,
        data = viewModel.duoStatsMap.entries.map { it.key to it.value }.sortedBy { (_, stats) -> - stats.total },
        title = "Most Common Lead",
        modifier = modifier,
    )
}

@Composable
internal fun MostEffectiveLeadCard(viewModel: LeadStatsViewModel, modifier: Modifier = Modifier) {
    LeadCard(
        viewModel = viewModel,
        data = viewModel.duoStatsMap.entries.map { it.key to it.value }.sortedBy { (_, stats) -> - stats.winRate },
        title = "Most Effective Lead",
        modifier = modifier,
    )
}

@Composable
internal fun LeadAndWin(viewModel: LeadStatsViewModel, modifier: Modifier = Modifier) {
    LeadCard(
        viewModel = viewModel,
        data = viewModel.pokemonStats.entries.map { listOf(it.key) to it.value }.sortedBy { (_, stats) -> - stats.winRate },
        title = "Lead And Win",
        modifier = modifier,
    )
}

@Composable
private fun LeadCard(
    viewModel: LeadStatsViewModel,
    data: List<Pair<List<PokemonName>, WinStats>>,
    title: String,
    modifier: Modifier) {
    StatCard(
        title = title,
        modifier = modifier,
        data = data,
    ) { (lead, stats) ->
        Spacer(Modifier.width(8.dp))
        lead.forEach { pokemon ->
            viewModel.pokemonImageService.PokemonSprite(pokemon, modifier = Modifier.size(statCardPokemonSpriteSize))
        }
        Text(
            text = if (stats.total == 0) "Did not lead"
            else "Won\n${stats.winCount} out of ${stats.total} games",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        PercentageText(stats.winRate)
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
fun PercentageText(rate: Float) {
    Text(
        "${rate.times(100).toInt()}%",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.width(statCardPercentageWidth),
        textAlign = TextAlign.Center,
    )
}