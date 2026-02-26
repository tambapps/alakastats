package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.matchup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.composables.PokemonStatCard
import com.tambapps.pokemon.alakastats.ui.composables.PokemonStatsRow
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.FiltersBar
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.Header
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.replay.NoReplay
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.theme.tabReplaysTextMarginTopMobile


@Composable
fun MatchupsTab(viewModel: MatchupsViewModel) {
    LaunchedEffect(viewModel.useCase.filters) {
        viewModel.loadStats()
    }
    if (!viewModel.isLoading && viewModel.hasNoData) {
        NoReplay(viewModel)
        return
    }
    val isCompact = LocalIsCompact.current

    val scrollState = rememberScrollState()
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(scrollState)
            .padding(if (isCompact) PaddingValues(horizontal = 8.dp) else PaddingValues(top = 16.dp, bottom = 8.dp))
    ) {
        val space = if (isCompact) 42.dp else 64.dp
        if (isCompact) {
            Spacer(Modifier.height(tabReplaysTextMarginTopMobile))
        }
        FiltersBar(viewModel)
        Spacer(Modifier.height(16.dp))
        Header(viewModel.useCase)
        BestMatchupsRow(viewModel)
        Spacer(Modifier.height(space))
        WorstMatchupsRow(viewModel)
    }
}

@Composable
fun BestMatchupsRow(viewModel: MatchupsViewModel) = PokemonStatsRow(
    viewModel = viewModel,
    title = "Best Matchups",
    stats = viewModel.bestMatchups,
    isDuo = false) { matchupStats ->
    val winCount = matchupStats.winCount
    val total = matchupStats.attendanceCount
    val text = when {
        winCount == 0 -> "Lost to all $total games"
        winCount == total && total == 1 -> "Beat\n1 out of 1\ngame"
        winCount == total -> "Beat all\n$total games"
        else -> "Beat ${winCount}\nout of ${total}\ngames"
    }
    PokemonStatCard(
        pokemonImageService = viewModel.pokemonImageService,
        title = "${matchupStats.winRate.times(100).toInt()}%",
        text = text,
        pokemonName = matchupStats.pokemonName,
        modifier = Modifier.size(256.dp).padding(bottom = 32.dp)
    )
}

@Composable
fun WorstMatchupsRow(viewModel: MatchupsViewModel) = PokemonStatsRow(
    viewModel = viewModel,
    title = "Worst Matchups",
    stats = viewModel.worstMatchups,
    isDuo = false) { matchupStats ->
    val looseCount = matchupStats.attendanceCount - matchupStats.winCount
    val total = matchupStats.attendanceCount
    val text = when {
        looseCount == 0 -> "Beat all $total games"
        looseCount == total && total == 1 -> "Lost to\n1 out of 1\ngame"
        looseCount == total -> "Lost to all\n$total games"
        else -> "Lost to ${looseCount}\nout of ${total}\ngames"
    }
    PokemonStatCard(
        pokemonImageService = viewModel.pokemonImageService,
        title = "${matchupStats.winRate.times(100).toInt()}%",
        text = text,
        pokemonName = matchupStats.pokemonName,
        modifier = Modifier.size(256.dp).padding(bottom = 32.dp)
    )
}

