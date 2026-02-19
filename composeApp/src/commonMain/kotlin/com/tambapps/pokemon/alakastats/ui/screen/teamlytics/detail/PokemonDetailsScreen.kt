package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.SnackBar
import com.tambapps.pokemon.alakastats.ui.composables.PokemonMoves
import com.tambapps.pokemon.alakastats.ui.composables.PokemonStatsRow
import com.tambapps.pokemon.alakastats.ui.composables.PokepastePokemonItemAndAbility
import com.tambapps.pokemon.alakastats.ui.composables.TabRowWithBackButton
import com.tambapps.pokemon.alakastats.ui.composables.elevatedCardGradientColors
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import org.koin.core.parameter.parametersOf
import kotlin.uuid.Uuid

data class PokemonDetailsScreen(
    val teamId: Uuid,
    val pokemonNameStr: String
): Screen {

    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<PokemonDetailViewModel> { parametersOf(teamId, PokemonName(pokemonNameStr)) }
        Box(
            modifier = Modifier
                .background(Brush.verticalGradient(elevatedCardGradientColors))
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            when (val state = viewModel.state) {
                is TeamPokemonStateState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp).align(Alignment.Center)
                    )
                }
                is TeamPokemonStateState.Error -> {
                    val navigator = LocalNavigator.currentOrThrow
                    val snackBar = LocalSnackBar.current
                    LaunchedEffect(Unit) {
                        snackBar.show("Error: ${state.error.message}", SnackBar.Severity.ERROR)
                        navigator.pop()
                    }
                }
                is TeamPokemonStateState.Loaded -> {
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(durationMillis = 1000))
                    ) {
                        PokemonDetails(viewModel, state)
                    }
                }
            }
        }
    }
}

@Composable
private fun PokemonDetails(
    viewModel: PokemonDetailViewModel,
    state: TeamPokemonStateState.Loaded
) {
    var dimensions by remember { mutableStateOf(0.dp to 0.dp) }
    val (contentWidth, _) = dimensions
    val density = LocalDensity.current
    val isCompact = LocalIsCompact.current
    val tabs = remember { getPagerTabs(isCompact) }
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    Column(Modifier.fillMaxSize()) {
        if (!isCompact) {
            TabRowWithBackButton(viewModel, pagerState, tabs, Modifier.fillMaxWidth())
        }
        Box(Modifier.fillMaxWidth().weight(1f)
            .onSizeChanged { size ->
                with(density) { dimensions = size.width.toDp() to size.height.toDp() }
            }
        ) {
            Pager(isCompact, viewModel, pagerState, state, Modifier.fillMaxSize())
            viewModel.pokemonImageService.PokemonArtwork(
                name = state.pokemon.name,
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 8.dp)
                    .height(if (LocalIsCompact.current) 175.dp else 250.dp)
                    // to avoid artworks like basculegion's to take the whole width and make the moves difficult to read
                    .widthIn(max = remember(contentWidth) { contentWidth * 0.75f })
                    .offset(y = 16.dp)
            )
        }
        if (isCompact) {
            TabRowWithBackButton(viewModel, pagerState, tabs, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Pager(
    isCompact: Boolean,
    viewModel: PokemonDetailViewModel,
    pagerState: PagerState,
    state: TeamPokemonStateState.Loaded,

    modifier: Modifier = Modifier
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier
    ) { page ->

        // TODO emit signal
        if (isCompact) {
            PokemonDetailsMobile(viewModel, state)
        } else {
            PokemonDetailsDesktop(viewModel, state)
        }
    }
}

private fun getPagerTabs(isCompact: Boolean) = buildList {
    add("Overview")
    if (isCompact) {
        add("Usage")
    }
    add("Speed Scale")
}

@Composable
internal fun PokemonDetailsOverview(
    viewModel: PokemonDetailViewModel,
    state: TeamPokemonStateState.Loaded,
    modifier: Modifier = Modifier
) {
    val (team, pokemon, pokemonData, notes) = state

    Column(modifier) {
        PokepastePokemonItemAndAbility(pokemon, viewModel.pokemonImageService)
        Spacer(Modifier.height(16.dp))
        if (!notes.isNullOrBlank()) {
            Text(notes, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
        }
        if (!team.pokePaste.isOts) {
            if (pokemonData != null) {
                Text("Stats (lvl ${pokemon.level})", style = MaterialTheme.typography.headlineSmall)
                PokemonStatsRow(pokemon, pokemonData, Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
            }
            Text("Investments", style = MaterialTheme.typography.headlineSmall)
            PokemonStatsRow(pokemon, pokemonData=null, Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }
        Text("Moves", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        PokemonMoves(pokemon, viewModel.pokemonImageService)
    }
}