package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.lead

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.ui.composables.FabLayout
import com.tambapps.pokemon.alakastats.ui.composables.PokemonCard
import com.tambapps.pokemon.alakastats.ui.composables.ScrollableRow
import com.tambapps.pokemon.alakastats.ui.composables.StatCard
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.FiltersButton
import com.tambapps.pokemon.alakastats.ui.service.FacingDirection
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.theme.statCardPercentageWidth
import com.tambapps.pokemon.alakastats.ui.theme.statCardPokemonSpriteSize
import kotlin.math.pow

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
    val cardInputs: List<Triple<PokemonName, PokemonName?, WinStats>> = remember(viewModel.pokemonStats) {
        viewModel.pokemonStats
            .entries
            .asSequence()
            .map { Triple(it.key, null, it.value) }
            .sortedWith(compareBy({ (_, _, stats) -> - stats.winRate }, { (_, _, stats) -> - stats.total }))
            .toList()
    }
    LeadRow(
        title = "Lead And Win",
        viewModel = viewModel,
        leadCardInputs = cardInputs,
        isDuo = false
    )
}

@Composable
internal fun MostEffectiveLeadRow(viewModel: LeadStatsViewModel) {
    DuoLeadRow(
        title = "Most Effective Leads",
        viewModel = viewModel,
        comparator = compareBy({ (_, _, stats) -> - stats.winRate }, { (_, _, stats) -> - stats.total })
    )
}

@Composable
internal fun MostCommonLeadRow(viewModel: LeadStatsViewModel) {
    DuoLeadRow(
        title = "Most Common Leads",
        viewModel = viewModel,
        comparator = compareBy({ (_, _, stats) -> - stats.total }, { (_, _, stats) -> - stats.winRate }),
    )
}

@Composable
private fun DuoLeadRow(
    title: String,
    viewModel: LeadStatsViewModel,
    comparator: Comparator<in Triple<PokemonName, PokemonName?, WinStats>>
) {
    val cardInputs: List<Triple<PokemonName, PokemonName?, WinStats>> = remember(viewModel.duoStatsMap) {
        viewModel.duoStatsMap
            .entries
            .asSequence()
            .map { Triple(it.key.first(), it.key[1], it.value) }
            .sortedWith(comparator)
            .toList()
    }
    LeadRow(
        title = title,
        viewModel = viewModel,
        leadCardInputs = cardInputs,
        isDuo = true
    )
}

@Composable
private fun LeadRow(
    viewModel: LeadStatsViewModel,
    title: String,
    leadCardInputs: List<Triple<PokemonName, PokemonName?, WinStats>>,
    isDuo: Boolean
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
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
            leadCardInputs.forEach { (pokemon1, pokemon2, stat) ->
                LeadCard(
                    viewModel = viewModel,
                    pokemonName = pokemon1,
                    pokemonName2 = pokemon2,
                    stat = stat,
                    modifier = Modifier.size(256.dp).padding(bottom = 32.dp)
                )
                Spacer(Modifier.width(spaceWidth))
            }
        }
    }
}

/**
 * Function given by ChatGPT: Power-law (gamma) mapping — easiest to tune
 * I wanted a function to offset a little Pokemons with a small width, and a lot Pokemon with large with,
 * here it is
 */
private fun leadOffsetDp(
    widthDp: Dp,
    minOffsetDp: Dp,     // thin sprites
    maxOffsetDp: Dp,     // very wide sprites
    wMinDp: Dp = 56.dp,          // tune to your art
    wMaxDp: Dp = 160.dp,         // tune to your art
    gamma: Double = 1.8
): Dp {
    val w = widthDp.value
    val wMin = wMinDp.value
    val wMax = wMaxDp.value
    val t = ((w - wMin) / (wMax - wMin)).coerceIn(0f, 1f)
    val eased = t.toDouble().pow(gamma).toFloat()
    val off = minOffsetDp.value + (maxOffsetDp.value - minOffsetDp.value) * eased
    return off.dp
}


@Composable
private fun LeadCard(
    viewModel: LeadStatsViewModel,
    pokemonName: PokemonName,
    pokemonName2: PokemonName? = null,
    stat: WinStats,
    modifier: Modifier) {
    val density = LocalDensity.current
    val (minOffsetDp, maxOffsetDp) = if (pokemonName2 == null) 12.dp  to 80.dp else 24.dp to 100.dp
    PokemonCard(
        modifier = modifier,
        pokemonArtwork = { contentWidth, contentHeight ->
            var spriteWidth by remember { mutableStateOf(0.dp) }
            viewModel.pokemonImageService.PokemonArtwork(
                name = pokemonName,
                modifier = Modifier.align(Alignment.BottomEnd)
                    .height(if (LocalIsCompact.current) 175.dp else 200.dp)
                    // to avoid artworks like basculegion's to take the whole width and make the moves difficult to read
                    .widthIn(max = remember(contentWidth) { contentWidth * 0.7f })
                    .onSizeChanged { size ->
                        with(density) { spriteWidth = size.width.toDp() }
                    }
                    .offset(y = 16.dp, x = leadOffsetDp(
                        spriteWidth,
                        minOffsetDp = minOffsetDp,
                        maxOffsetDp = maxOffsetDp
                    ))
            )
            pokemonName2?.let {
                var spriteWidth by remember { mutableStateOf(0.dp) }

                viewModel.pokemonImageService.PokemonArtwork(
                    name = it,
                    facingDirection = FacingDirection.RIGHT,
                    modifier = Modifier.align(Alignment.BottomStart)
                        .height(if (LocalIsCompact.current) 175.dp else 200.dp)
                        // to avoid artworks like basculegion's to take the whole width and make the moves difficult to read
                        .widthIn(max = remember(contentWidth) { contentWidth * 0.7f })
                        .onSizeChanged { size ->
                            with(density) { spriteWidth = size.width.toDp() }
                        }
                        .offset(y = 16.dp, x = - leadOffsetDp(
                            spriteWidth,
                            minOffsetDp = minOffsetDp,
                            maxOffsetDp = maxOffsetDp
                        ))
                )
            }
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
                else -> "Won ${winCount}\nout of ${stat.total}\ngames"
            },
            textAlign = if (pokemonName2 != null) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.align(if (pokemonName2 != null) Alignment.BottomCenter else Alignment.BottomStart).padding(bottom = 8.dp),
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

// TODO delete below
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