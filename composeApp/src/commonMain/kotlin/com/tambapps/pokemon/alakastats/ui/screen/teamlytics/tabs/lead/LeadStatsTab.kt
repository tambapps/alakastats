package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.lead

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
import com.tambapps.pokemon.alakastats.ui.composables.PokemonCard
import com.tambapps.pokemon.alakastats.ui.composables.ScrollableRow
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.ScrollToTopIfNeeded
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.replay.NoReplay
import com.tambapps.pokemon.alakastats.ui.service.FacingDirection
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import kotlin.math.pow

@Composable
fun LeadStatsTab(viewModel: LeadStatsViewModel) {
    val isCompact = LocalIsCompact.current
    LaunchedEffect(viewModel.useCase.filters) {
        viewModel.loadStats()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        val scrollState = rememberScrollState()
        if (!viewModel.isLoading && viewModel.hasNoData) {
            NoReplay(viewModel)
        } else if (isCompact) {
            LeadStatsTabMobile(viewModel, scrollState)
        } else {
            LeadStatsTabDesktop(viewModel, scrollState)
        }
        ScrollToTopIfNeeded(viewModel, scrollState)
    }
}


@Composable
internal fun LeadAndWinRow(viewModel: LeadStatsViewModel) {
    LeadRow(
        title = "Lead And Win",
        viewModel = viewModel,
        leadStats = viewModel.leadAndWinStats,
    )
}

@Composable
internal fun MostEffectiveLeadRow(viewModel: LeadStatsViewModel) {
    LeadRow(
        title = "Most Effective Leads",
        viewModel = viewModel,
        leadStats = viewModel.mostEffectiveLeadsStats,
    )
}

@Composable
internal fun MostCommonLeadRow(viewModel: LeadStatsViewModel) {
    LeadRow(
        title = "Most Common Leads",
        viewModel = viewModel,
        leadStats = viewModel.mostCommonLeadsStats,
    )
}

@Composable
private fun LeadRow(
    viewModel: LeadStatsViewModel,
    title: String,
    leadStats: List<LeadStats>,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        val isDuo = leadStats.firstOrNull()?.lead?.size?.let { it >= 2 } == true
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
            leadStats.forEach { leadStat ->
                LeadCard(
                    viewModel = viewModel,
                    pokemonName = leadStat.lead.first(),
                    pokemonName2 = leadStat.lead.getOrNull(1),
                    stat = leadStat.stats,
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
