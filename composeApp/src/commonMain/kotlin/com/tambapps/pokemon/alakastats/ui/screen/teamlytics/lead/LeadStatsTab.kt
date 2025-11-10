package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.lead

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.ui.composables.FabLayout
import com.tambapps.pokemon.alakastats.ui.composables.PokemonCard
import com.tambapps.pokemon.alakastats.ui.composables.StatCard
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.FiltersButton
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.theme.statCardPercentageWidth
import com.tambapps.pokemon.alakastats.ui.theme.statCardPokemonSpriteSize

@Composable
fun LeadStatsTab(viewModel: LeadStatsViewModel) {
    val isCompact = LocalIsCompact.current
    LaunchedEffect(viewModel.useCase.filters) {
        viewModel.loadStats()
    }
    FabLayout(
        fab = {
            FiltersButton(viewModel.useCase)
        }
    ) {
        if (!viewModel.isLoading && viewModel.duoStatsMap.isEmpty() && viewModel.pokemonStats.isEmpty()) {
            NoData(viewModel)
        } else if (isCompact) {
            LeadStatsTabMobile(viewModel)
        } else {
            LeadStatsTabDesktop(viewModel)
        }
    }
}


@Composable
internal fun LeadAndWinRow(viewModel: LeadStatsViewModel) {
    Text(
        text = "Lead And Win",
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold
    )

    val entries = remember(viewModel.pokemonStats) {
        viewModel.pokemonStats.entries.asSequence().map { it.key to it.value }.sortedBy { (_, stats) -> - stats.winRate }
            .toList()
    }
    val isCompact = LocalIsCompact.current
    Row(
        Modifier.fillMaxWidth().then(
                if (isCompact) Modifier.horizontalScroll(rememberScrollState()) else Modifier
            )
    ) {
        entries.forEach { (pokemonName, stat) ->
            LeadAndWinCard(viewModel, pokemonName, stat,
                modifier = if (isCompact) Modifier.size(256.dp) else Modifier.weight(1f)
            )
            Spacer(Modifier.width(64.dp))
        }
    }
}

@Composable
private fun LeadAndWinCard(viewModel: LeadStatsViewModel, pokemonName: PokemonName, stat: WinStats, modifier: Modifier) {

    PokemonCard(
        modifier = modifier,
        pokemonArtwork = { contentWidth, contentHeight ->
            viewModel.pokemonImageService.PokemonArtwork(
                name = pokemonName,
                modifier = Modifier.align(Alignment.BottomEnd)
                    .height(if (LocalIsCompact.current) 175.dp else 200.dp)
                    // to avoid artworks like basculegion's to take the whole width and make the moves difficult to read
                    .widthIn(max = remember(contentWidth) { contentWidth * 0.7f })
                    .offset(y = 16.dp, x = 32.dp)
            )
        }
    ) {
        Text(
            "${stat.winRate.times(100).toInt()}%",
            modifier = Modifier.align(Alignment.TopCenter),
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center,
        )

        val winCount = stat.winCount
        val total = stat.total
        Text(
            when {
                total == 0 -> "Did not participate\nto any game"
                winCount == 0 -> "Lost all $total games"
                winCount == total && total == 1 -> "Won\n1 out of 1\ngame"
                winCount == total -> "Won all\n$total games"
                else -> "Won\n${winCount} out of ${stat.total}\ngames"
            },
            modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 8.dp),
            style = MaterialTheme.typography.headlineMedium
        )
    }
}


@Composable
private fun NoData(viewModel: LeadStatsViewModel) {
    Box(Modifier.fillMaxSize()) {
        Text(if (!viewModel.useCase.hasFilteredReplays) "No data" else "No replays matched the filters", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Center))
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