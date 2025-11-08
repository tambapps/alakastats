package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.move

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.cardGradientColors
import com.tambapps.pokemon.alakastats.ui.composables.elevatedCardGradientColors
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import io.github.koalaplot.core.pie.PieChart
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import kotlin.collections.component1
import kotlin.collections.component2

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

internal val MoveUsageViewModel.sortedPokemonMovesUsageEntries get() =
    pokemonMovesUsage.entries.toList()
    .sortedBy { (pName, _) ->
        val i = team.pokePaste.pokemons.indexOfFirst { p -> p.name.matches(pName) }
        if (i != -1) i else Int.MAX_VALUE
    }

@Composable
internal fun PokemonMoveUsageCard(
    viewModel: MoveUsageViewModel,
    name: PokemonName,
    moveUsage: MovesUsage,
    modifier: Modifier = Modifier,
    ) {
    MyCard(
        modifier = modifier.padding(horizontal = 8.dp),
        gradientBackgroundColors = cardGradientColors
    ) {
        Spacer(Modifier.height(4.dp))
        PokemonMoveUsageDonut(viewModel, name, moveUsage, Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        if (moveUsage.movesCount.isNotEmpty()) {
            Text("Participated in ${moveUsage.replaysCount} games", modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(Modifier.height(8.dp))
        }
    }
}

private const val MOVE_STRUGGLE = "struggle"
@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
internal fun PokemonMoveUsageDonut(
    viewModel: MoveUsageViewModel,
    name: PokemonName,
    moveUsage: MovesUsage,
    modifier: Modifier = Modifier,
) {
    val rawEntries = moveUsage.movesCount.entries.filter { PokemonNormalizer.normalize(it.key) != MOVE_STRUGGLE }
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
