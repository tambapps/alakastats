package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.lead

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.composables.StatCard
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.theme.statCardPokemonSpriteSize

@Composable
fun LeadStatsTab(viewModel: LeadStatsViewModel) {
    val isCompact = LocalIsCompact.current
    LaunchedEffect(Unit) {
        viewModel.loadStats()
    }
    // TODO handle no stats and not loading case
    if (isCompact) {
        LeadStatsTabMobile(viewModel)
    } else {
        LeadStatsTabDesktop(viewModel)
    }
}

@Composable
internal fun MostCommonLeadCard(viewModel: LeadStatsViewModel) {
    LeadCard(
        viewModel = viewModel,
        title = "Most Common Lead"
    ) { (_, stats) -> - stats.total }
}

@Composable
internal fun MostEffectiveLeadCard(viewModel: LeadStatsViewModel) {
    LeadCard(
        viewModel = viewModel,
        title = "Most Effective Lead"
    ) { (_, stats) -> - stats.winRate }
}

@Composable
private inline fun <R : Comparable<R>> LeadCard(
    viewModel: LeadStatsViewModel,
    title: String,
    crossinline sortCriteria: (Map.Entry<List<String>, WinStats>) -> R?) {
    StatCard(
        title = title,
        viewModel.duoStatsMap.entries.sortedBy(sortCriteria)
    ) { (lead, stats) ->
        Spacer(Modifier.width(8.dp))
        lead.forEach { pokemon ->
            viewModel.pokemonImageService.PokemonSprite(pokemon, modifier = Modifier.size(statCardPokemonSpriteSize))
        }
        Text("Won\n${stats.winCount} out of ${stats.total}", modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("${stats.winRate.times(100).toInt()}%", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(8.dp))
    }
}