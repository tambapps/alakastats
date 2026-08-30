@file:OptIn(ExperimentalMaterial3Api::class)

package com.tambapps.pokemon.alakastats.ui.screen.manualreplay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.SnackBar
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton
import com.tambapps.pokemon.alakastats.ui.composables.FabLayout
import com.tambapps.pokemon.alakastats.ui.composables.LazyColumnWithScrollbar
import com.tambapps.pokemon.alakastats.ui.service.ItemImage
import com.tambapps.pokemon.alakastats.ui.service.PokemonSprite
import com.tambapps.pokemon.alakastats.ui.service.availablePokemonNames
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.util.MegaUtils
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

data class ManualReplayScreen(
    val team: Teamlytics,
    val replayToEdit: ReplayAnalytics? = null,
    val onReplaySaved: (ReplayAnalytics) -> Unit
) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<ManualReplayViewModel> { parametersOf(team) }
        val navigator = LocalNavigator.currentOrThrow
        val snackBar = LocalSnackBar.current
        val scope = rememberCoroutineScope()
        val pages = ManualReplayPage.entries
        val pagerState = rememberPagerState(pageCount = { pages.size })
        LaunchedEffect(Unit) {
            if (replayToEdit != null) {
                viewModel.prepareEdition(replayToEdit)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (replayToEdit != null) "Edit Replay" else "Add Replay Manually") },
                    navigationIcon = { BackIconButton(navigator) }
                )
            }
        ) { scaffoldPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(scaffoldPadding)) {
                FabLayout(
                    fab = {
                        DoneButton(enabled = viewModel.canBuildReplayAnalytics) {
                            val error = viewModel.validationError
                            if (error == null) {
                                onReplaySaved(viewModel.buildReplayAnalytics())
                                navigator.pop()
                            } else {
                                // bring the user where the missing information is asked
                                scope.launch { pagerState.animateScrollToPage(error.page.ordinal) }
                                snackBar.show(error.message, SnackBar.Severity.ERROR)
                            }
                        }
                    }
                ) {
                    Column(Modifier.fillMaxSize()) {
                        SecondaryTabRow(selectedTabIndex = pagerState.currentPage) {
                            pages.forEachIndexed { index, page ->
                                Tab(
                                    selected = pagerState.currentPage == index,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                    text = { Text(page.title) }
                                )
                            }
                        }
                        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                            // so that no page starts stuck to the tabs
                            Box(Modifier.fillMaxSize().padding(top = 16.dp)) {
                                when (pages[page]) {
                                    ManualReplayPage.YOU -> YouPage(viewModel)
                                    ManualReplayPage.OPPONENT -> OpponentPage(viewModel)
                                    ManualReplayPage.NOTES -> NotesPage(viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (viewModel.showSelectPokemonDialog) {
            SelectPokemonDialog(viewModel)
        }
        viewModel.megaStoneSelectionFor?.let {
            SelectMegaStoneDialog(viewModel, it)
        }
    }
}

@Composable
private fun DoneButton(enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    // FABs have no enabled flag, the disabled look has to be done through the colors. It stays
    // clickable on purpose, to tell the user what is missing
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor =
            if (enabled) FloatingActionButtonDefaults.containerColor
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        contentColor =
            if (enabled) contentColorFor(FloatingActionButtonDefaults.containerColor)
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    ) {
        Text("Done")
    }
}

// both pages display pokemon cards the same way, only the cards themselves differ
@Composable
internal fun <T> ManualPokemonGrid(
    pokemonStates: List<T>,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
    addCard: @Composable ((Modifier) -> Unit)? = null,
    card: @Composable (T, Modifier) -> Unit
) {
    if (LocalIsCompact.current) {
        ManualPokemonGridMobile(pokemonStates, modifier, header, addCard, card)
    } else {
        ManualPokemonGridDesktop(pokemonStates, modifier, header, addCard, card)
    }
}

@Composable
private fun SelectMegaStoneDialog(viewModel: ManualReplayViewModel, pokemonState: ManualPokemonState) {
    val megaStones = remember(pokemonState.name) { MegaUtils.getMegaStones(pokemonState.name).toList() }

    AlertDialog(
        onDismissRequest = { viewModel.hideMegaStoneSelectionDialog() },
        title = { Text("Select Mega Stone") },
        text = {
            Column {
                megaStones.forEach { megaStone ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectMegaStone(pokemonState, megaStone) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ItemImage(megaStone, Modifier.size(40.dp))
                        Text(megaStone.pretty)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { viewModel.hideMegaStoneSelectionDialog() }) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SelectPokemonDialog(viewModel: ManualReplayViewModel) {
    val allPokemons = availablePokemonNames()
    var text by remember { mutableStateOf("") }
    val pokemons = remember(text, viewModel.opponentPokemonStates.size) {
        allPokemons.filter {
            !viewModel.containsOpponentPokemon(it) &&
                    (text.isBlank() || it.value.contains(text, ignoreCase = true))
        }
    }

    AlertDialog(
        onDismissRequest = { viewModel.hideSelectPokemonDialog() },
        title = { Text("Select Pokemon") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Pokemon Name") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                LazyColumnWithScrollbar(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(pokemons) { pokemon ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.addOpponentPokemon(pokemon) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PokemonSprite(pokemon, Modifier.size(40.dp))
                            Text(pokemon.pretty)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { viewModel.hideSelectPokemonDialog() }) {
                Text("Cancel")
            }
        }
    )
}
