@file:OptIn(ExperimentalMaterial3Api::class)

package com.tambapps.pokemon.alakastats.ui.screen.manualreplay

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.add
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton
import com.tambapps.pokemon.alakastats.ui.composables.FabLayout
import com.tambapps.pokemon.alakastats.ui.composables.LazyColumnWithScrollbar
import com.tambapps.pokemon.alakastats.ui.composables.MegaSwitch
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.cardGradientColors
import com.tambapps.pokemon.alakastats.ui.service.FacingDirection
import com.tambapps.pokemon.alakastats.ui.service.PokemonSprite
import com.tambapps.pokemon.alakastats.ui.service.availablePokemonNames
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import org.jetbrains.compose.resources.painterResource

data class ManualReplayScreen(
    val onReplayAdded: (ReplayAnalytics) -> Unit
) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<ManualReplayViewModel>()
        val navigator = LocalNavigator.currentOrThrow
        val isCompact = LocalIsCompact.current

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Add Replay Manually") },
                    navigationIcon = { BackIconButton(navigator) }
                )
            }
        ) { scaffoldPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(scaffoldPadding)) {
                FabLayout(
                    fab = { AddPokemonButton(viewModel) }
                ) {
                    if (viewModel.pokemonStates.isEmpty()) {
                        NoPokemon()
                    } else if (isCompact) {
                        ManualReplayScreenMobile(viewModel)
                    } else {
                        ManualReplayScreenDesktop(viewModel)
                    }
                }
            }
        }

        if (viewModel.showSelectPokemonDialog) {
            SelectPokemonDialog(viewModel)
        }
    }
}

@Composable
private fun AddPokemonButton(viewModel: ManualReplayViewModel, modifier: Modifier = Modifier) {
    if (!viewModel.canAddPokemon) {
        return
    }
    FloatingActionButton(
        onClick = { viewModel.showSelectPokemonDialog() },
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(Res.drawable.add),
            contentDescription = "Add Pokemon",
        )
    }
}

@Composable
private fun NoPokemon() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("No Pokemon were added yet")
    }
}

@Composable
internal fun ManualPokemonCard(
    viewModel: ManualReplayViewModel,
    pokemonState: ManualPokemonState,
    modifier: Modifier = Modifier
) {
    MyCard(
        modifier = modifier,
        gradientBackgroundColors = cardGradientColors,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PokemonSprite(
                    pokemonState.displayedName,
                    Modifier.size(56.dp),
                    facingDirection = FacingDirection.RIGHT
                )
                Spacer(Modifier.width(8.dp))
                AnimatedContent(
                    targetState = pokemonState.displayedName.pretty,
                    transitionSpec = { (fadeIn() + slideInVertically { it }) togetherWith (fadeOut() + slideOutVertically { -it }) },
                    modifier = Modifier.weight(1f)
                ) { name ->
                    Text(name, style = MaterialTheme.typography.titleMedium)
                }
                if (pokemonState.canMega) {
                    MegaSwitch(
                        megaSelected = pokemonState.megaSelected,
                        onCheckedChange = { viewModel.updateMegaSelected(pokemonState, it) }
                    )
                }
                TextButton(onClick = { viewModel.removePokemon(pokemonState) }) {
                    Text("×", style = MaterialTheme.typography.titleLarge)
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = pokemonState.item,
                onValueChange = pokemonState::updateItem,
                label = { Text("Item") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            pokemonState.moves.forEachIndexed { index, move ->
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = move,
                    onValueChange = { pokemonState.updateMove(index, it) },
                    label = { Text("Move ${index + 1}") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        }
    }
}

@Composable
private fun SelectPokemonDialog(viewModel: ManualReplayViewModel) {
    val allPokemons = availablePokemonNames()
    var text by remember { mutableStateOf("") }
    val pokemons = remember(text, viewModel.pokemonStates.size) {
        allPokemons.filter {
            !viewModel.contains(it) && (text.isBlank() || it.value.contains(text, ignoreCase = true))
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
                                .clickable { viewModel.addPokemon(pokemon) }
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
