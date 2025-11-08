package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.move

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.PokemonNormalizer
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.cardGradientColors
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import io.github.koalaplot.core.pie.PieChart
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import kotlin.collections.component1
import kotlin.collections.component2

@Composable
fun UsagesTab(viewModel: UsagesViewModel) {
    val isCompact = LocalIsCompact.current
    LaunchedEffect(Unit) {
        viewModel.loadStats()
    }
    if (!viewModel.isLoading && viewModel.pokemonPokemonUsages.isEmpty()) {
        NoData()
    } else if (isCompact) {
        UsagesTabMobile(viewModel)
    } else {
        UsagesTabDesktop(viewModel)
    }
}

internal val UsagesViewModel.sortedPokemonMovesUsageEntries get() =
    pokemonPokemonUsages.entries.toList()
    .sortedBy { (pName, _) ->
        val i = team.pokePaste.pokemons.indexOfFirst { p -> p.name.matches(pName) }
        if (i != -1) i else Int.MAX_VALUE
    }

@Composable
internal fun ReplayCountText(viewModel: UsagesViewModel) {

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            "${viewModel.replays.size} replays",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )
    }
}

@Composable
internal fun PokemonUsagesCard(
    viewModel: UsagesViewModel,
    name: PokemonName,
    usages: PokemonUsages,
    modifier: Modifier = Modifier,
    ) {
    MyCard(
        modifier = modifier.padding(horizontal = 8.dp),
        gradientBackgroundColors = cardGradientColors
    ) {
        Spacer(Modifier.height(4.dp))
        Text(name.pretty, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 8.dp))
        Spacer(Modifier.height(8.dp))
        val replays = viewModel.replays
        if (replays.isNotEmpty()) {
            val winRate = usages.winCount * 100 / usages.usageCount
            val usageRate = usages.usageCount * 100 / viewModel.replays.size
            Row(
                Modifier.padding(horizontal = 8.dp)
            ) {
                Spacer(Modifier.weight(1f))
                Text("$winRate% winrate", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(32.dp))
                Text("$usageRate% usage", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }
        PokemonUsagesDonut(viewModel, name, usages, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))
        if (usages.movesCount.isNotEmpty()) {
            Column(Modifier.padding(horizontal = 8.dp)) {
                usages.run {
                    Text("Participated in $usageCount games", fontSize = 18.sp, modifier = Modifier.alpha(0.9f))
                    Text("Won $winCount of them", fontSize = 16.sp, modifier = Modifier.alpha(0.9f))
                    Text("Tera-ed in $teraCount games and won $teraAndWinCount of them", fontSize = 16.sp, modifier = Modifier.alpha(0.75f))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private const val MOVE_STRUGGLE = "struggle"
@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
private fun PokemonUsagesDonut(
    viewModel: UsagesViewModel,
    name: PokemonName,
    usages: PokemonUsages,
    modifier: Modifier = Modifier,
) {
    val rawEntries = usages.movesCount.entries.filter { PokemonNormalizer.normalize(it.key) != MOVE_STRUGGLE }
    val total = remember { rawEntries.sumOf { it.value } }
    val entries = remember {
        rawEntries.map {
            Triple(PokemonNormalizer.pretty(it.key), it.value, it.value * 100 / total)
        }
    }

    val diameter = if (LocalIsCompact.current) 150.dp else 200.dp
    PieChart(
        entries.map { it.second.toFloat() },
        modifier = modifier,
        label = { i ->
            val (moveName, count, percentage) = entries[i]
            Text("$moveName\n$count ($percentage%)", textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
        },
        holeSize = 0.75F,
        forceCenteredPie = true,
        minPieDiameter = diameter,
        maxPieDiameter = diameter,
        holeContent = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                viewModel.pokemonImageService.PokemonSprite(name)
            }
        }
    )
}

@Composable
private fun NoData() {
    Box(Modifier.fillMaxSize()) {
        Text("No data", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Center))
    }
}
