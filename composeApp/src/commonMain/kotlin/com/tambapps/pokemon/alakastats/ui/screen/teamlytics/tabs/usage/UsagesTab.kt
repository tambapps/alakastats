package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.usage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tambapps.pokemon.MoveName
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.PokemonNormalizer
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.ScrollToTopIfNeeded
import com.tambapps.pokemon.alakastats.ui.composables.cardGradientColors
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.replay.NoReplay
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import io.github.koalaplot.core.pie.PieChart
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import kotlin.collections.component1
import kotlin.collections.component2

@Composable
fun UsagesTab(viewModel: UsagesViewModel) {
    val isCompact = LocalIsCompact.current
    LaunchedEffect(viewModel.useCase.filters) {
        viewModel.loadStats()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        val scrollState = rememberScrollState()
        if (!viewModel.isLoading && viewModel.pokemonPokemonUsages.isEmpty()) {
            NoReplay(viewModel)
        } else if (isCompact) {
            UsagesTabMobile(viewModel, scrollState)
        } else {
            UsagesTabDesktop(viewModel, scrollState)
        }
        ScrollToTopIfNeeded(viewModel, scrollState)
    }
}

@Composable
internal fun ColumnScope.OnlyPokePasteMovesSwitch(viewModel: UsagesViewModel) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = { viewModel.onlyPokePasteMoves = !viewModel.onlyPokePasteMoves })
            .align(Alignment.CenterHorizontally)
    ) {
        Switch(
            checked = viewModel.onlyPokePasteMoves,
            onCheckedChange = { viewModel.onlyPokePasteMoves = it },
        )
        Spacer(Modifier.padding(horizontal = 8.dp))
        Text("Only PokePaste\nMoves")
    }
}

internal val UsagesViewModel.sortedPokemonMovesUsageEntries get() =
    pokemonPokemonUsages.entries.toList()
    .sortedBy { (pName, _) ->
        val i = team.pokePaste.pokemons.indexOfFirst { p -> p.name.matches(pName) }
        if (i != -1) i else Int.MAX_VALUE
    }

@Composable
fun PokemonUsagesCard(
    pokemonImageService: PokemonImageService,
    replays: List<ReplayAnalytics>,
    name: PokemonName,
    usages: PokemonUsages,
    title: String = name.pretty,
    gradientBackgroundColors: List<Color>? = cardGradientColors,
    modifier: Modifier = Modifier,
    ) {
    MyCard(
        modifier = modifier.padding(horizontal = 8.dp),
        gradientBackgroundColors = gradientBackgroundColors
    ) {
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 8.dp))
        Spacer(Modifier.height(8.dp))
        if (replays.isNotEmpty()) {
            val winRate = if (usages.usageCount > 0) usages.winCount * 100 / usages.usageCount else 0
            val usageRate = usages.usageCount * 100 / replays.size
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
        PokemonUsagesDonut(pokemonImageService, name, usages, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))
        Column(Modifier.padding(horizontal = 8.dp)) {
            usages.run {
                Text(when(usageCount) {
                    0 -> "Did not participated in any games"
                    1 -> "Participated in 1 game"
                    else -> "Participated in $usageCount games"
                }, fontSize = 18.sp, modifier = Modifier.alpha(0.9f))
                Text(when {
                    usageCount == 1 && winCount == 1 -> "Won it"
                    usageCount == 1 -> "Did not win it"
                    winCount == usageCount -> "Won all of them"
                    winCount == 0 -> "Won none of them"
                    else -> "Won $winCount of them"
                }, fontSize = 16.sp, modifier = Modifier.alpha(0.9f))
                Text(when {
                    teraCount == 0 -> "Did not tera"
                    teraCount == 1 && teraAndWinCount == 0 -> "Tera-ed in 1 game and did not win it"
                    teraCount == 1 -> "Tera-ed in 1 game and won it"
                    teraCount == teraAndWinCount -> "Tera-ed in $teraCount games and won all of them"
                    teraAndWinCount == 0 -> "Tera-ed in $teraCount games and won none of them"
                    else -> "Tera-ed in $teraCount games and won $teraAndWinCount of them"
                }, fontSize = 16.sp, modifier = Modifier.alpha(0.75f))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private val MOVE_STRUGGLE = MoveName("struggle")
@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
private fun PokemonUsagesDonut(
    pokemonImageService: PokemonImageService,
    name: PokemonName,
    usages: PokemonUsages,
    modifier: Modifier = Modifier,
) {
    val rawEntries = usages.movesCount.entries.filter { !it.key.matches(MOVE_STRUGGLE) }
    val total = rawEntries.sumOf { it.value }
    val entries = rawEntries.map {
        val percentage = if (total > 0) it.value * 100 / total else 0
        Triple(it.key.pretty, it.value, percentage)
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
                pokemonImageService.PokemonSprite(name)
            }
        }
    )
}