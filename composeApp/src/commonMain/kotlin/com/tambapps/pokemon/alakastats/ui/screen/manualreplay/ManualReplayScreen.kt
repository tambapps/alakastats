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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.contentColorFor
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
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton
import com.tambapps.pokemon.alakastats.ui.composables.FabLayout
import com.tambapps.pokemon.alakastats.ui.composables.LazyColumnWithScrollbar
import com.tambapps.pokemon.alakastats.ui.composables.MegaSwitch
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.cardGradientColors
import com.tambapps.pokemon.alakastats.ui.service.FacingDirection
import com.tambapps.pokemon.alakastats.ui.service.ItemImage
import com.tambapps.pokemon.alakastats.ui.service.PokemonSprite
import com.tambapps.pokemon.alakastats.ui.service.availablePokemonNames
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.theme.defaultIconColor
import com.tambapps.pokemon.util.MegaUtils
import org.jetbrains.compose.resources.painterResource
import org.koin.core.parameter.parametersOf

data class ManualReplayScreen(
    val team: Teamlytics,
    val onReplayAdded: (ReplayAnalytics) -> Unit
) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<ManualReplayViewModel> { parametersOf(team) }
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
                    fab = {
                        DoneButton(viewModel) {
                            onReplayAdded(viewModel.buildReplayAnalytics())
                            navigator.pop()
                        }
                    }
                ) {
                    if (isCompact) {
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
        viewModel.megaStoneSelectionFor?.let {
            SelectMegaStoneDialog(viewModel, it)
        }
    }
}

@Composable
private fun DoneButton(viewModel: ManualReplayViewModel, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val enabled = viewModel.pokemonStates.isNotEmpty()
    // FABs have no enabled flag, the disabled look has to be done through the colors
    ExtendedFloatingActionButton(
        onClick = { if (enabled) onClick() },
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

// takes the place of the next pokemon card, so that adding one stays where the eye already is
@Composable
internal fun AddPokemonCard(viewModel: ManualReplayViewModel, modifier: Modifier = Modifier) {
    MyCard(
        modifier = modifier,
        onClick = { viewModel.showSelectPokemonDialog() },
        gradientBackgroundColors = cardGradientColors,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(Res.drawable.add),
                contentDescription = "Add Pokemon",
                tint = MaterialTheme.colorScheme.defaultIconColor,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text("Add Pokemon", style = MaterialTheme.typography.titleMedium)
        }
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
                // the item is the mega stone it mega evolved with
                readOnly = pokemonState.megaSelected,
            )
        }
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
