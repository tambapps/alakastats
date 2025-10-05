package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.move

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.PokemonNormalizer
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import io.github.koalaplot.core.pie.PieChart
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import kotlin.math.roundToInt

@Composable
fun MoveUsageTab(viewModel: MoveUsageViewModel) {
    val isCompact = LocalIsCompact.current
    LaunchedEffect(Unit) {
        viewModel.loadStats()
    }
    if (!viewModel.isLoading && viewModel.pokemonMovesUsage.isEmpty()) {
        NoData()
    } else if (isCompact) {
        MoveUsageTabMobile(viewModel)
    } else {
        MoveUsageTabDesktop(viewModel)
    }
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
internal fun PokemonMoveUsageDonut(
    viewModel: MoveUsageViewModel,
    modifier: Modifier,
    name: PokemonName,
    moveUsage: Map<String, Int>
) {
    val total = remember { moveUsage.entries.sumOf { it.value } }
    val entries = remember {
        moveUsage.entries.map {
            Triple(PokemonNormalizer.pretty(it.key), it.value, it.value * 100 / total)
        }
    }
    PieChart(
        entries.map { it.second.toFloat() },
        modifier = modifier,
        label = { i ->
            val (moveName, _, percentage) = entries[i]
            Text("$moveName\n$percentage%", textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
        },
        holeSize = 0.75F,
        forceCenteredPie = true,
        minPieDiameter = 150.dp,
        maxPieDiameter = 150.dp,
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
