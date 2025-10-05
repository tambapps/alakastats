package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.usage

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
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.Terastallization
import com.tambapps.pokemon.alakastats.ui.composables.StatCard
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.theme.statCardPercentageWidth
import com.tambapps.pokemon.alakastats.ui.theme.statCardPokemonSpriteSize

@Composable
fun UsageStatsTab(viewModel: UsageStatsViewModel) {
    val isCompact = LocalIsCompact.current
    LaunchedEffect(Unit) {
        viewModel.loadStats()
    }
    // TODO handle no stats case
    if (isCompact) {
        UsageStatsTabMobile(viewModel)
    } else {
        UsageStatsTabDesktop(viewModel)
    }
}

@Composable
internal fun UsageCard(
    viewModel: UsageStatsViewModel,
    modifier: Modifier = Modifier
) {
    StatCard(
        title = "Usage",
        modifier = modifier,
        data = viewModel.pokemonUsageMap.entries.sortedWith(
            compareBy<Map.Entry<PokemonName, UsageStat>> { (_, stat) -> - stat.usageRate }
                .thenBy { (_, stat) -> - stat.totalGames }
                .thenBy { (name, _) -> name }
        ),
    ) { (pokemon, stats) ->
        Spacer(Modifier.width(8.dp))
        viewModel.pokemonImageService.PokemonSprite(pokemon, modifier = Modifier.size(statCardPokemonSpriteSize))
        Text(
            text = if (stats.totalGames == 0) "No usage"
            else "Participated in\n${stats.usage} out of ${stats.totalGames} games",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        PercentageText(stats.usageRate)
        Spacer(Modifier.width(8.dp))
    }
}


@Composable
internal fun UsageAndWinCard(
    viewModel: UsageStatsViewModel,
    modifier: Modifier = Modifier
) {
    StatCard(
        title = "Usage And Win",
        modifier = modifier,
        data = viewModel.pokemonUsageAndWinMap.entries.sortedWith(
            compareBy<Map.Entry<PokemonName, UsageStat>> { (_, stat) -> - stat.usageRate }
                .thenBy { (_, stat) -> - stat.totalGames }
                .thenBy { (name, _) -> name }
        ),
    ) { (pokemon, stats) ->
        Spacer(Modifier.width(8.dp))
        viewModel.pokemonImageService.PokemonSprite(pokemon, modifier = Modifier.size(statCardPokemonSpriteSize))
        Text(
            text = if (stats.totalGames == 0) "No usage"
            else "Won in\n${stats.usage} out of ${stats.totalGames} games",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        PercentageText(stats.usageRate)
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
internal fun TeraAndWinCard(
    viewModel: UsageStatsViewModel,
    modifier: Modifier = Modifier
) {
    StatCard(
        title = "Tera And Win",
        modifier = modifier,
        data = viewModel.teraAndWinMap.entries.sortedWith(
            compareBy<Map.Entry<Terastallization, UsageStat>> { (_, stat) -> - stat.usageRate }
                .thenBy { (_, stat) -> - stat.totalGames }
                .thenBy { (pokeTera, _) -> pokeTera.pokemon }
                .thenBy { (pokeTera, _) -> pokeTera.type }

        ),
    ) { (pokeTera, stats) ->
        Spacer(Modifier.width(8.dp))
        viewModel.pokemonImageService.PokemonSprite(pokeTera.pokemon, modifier = Modifier.size(statCardPokemonSpriteSize))
        viewModel.pokemonImageService.TeraTypeImage(pokeTera.type, modifier = Modifier.size(statCardPokemonSpriteSize))
        Text(
            text = if (stats.totalGames == 0) "Did not tera"
            else "Won \n${stats.usage} out of ${stats.totalGames} games",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        PercentageText(stats.usageRate)
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