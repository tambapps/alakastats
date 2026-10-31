@file:OptIn(ExperimentalMaterial3Api::class)

package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.gameplan.edit

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.add
import alakastats.composeapp.generated.resources.content_paste
import alakastats.composeapp.generated.resources.delete
import alakastats.composeapp.generated.resources.edit
import alakastats.composeapp.generated.resources.visibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.tambapps.pokemon.alakastats.domain.model.MatchupPlan
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.withContext
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.PokePasteInput
import com.tambapps.pokemon.alakastats.ui.composables.PokemonFilterChip
import com.tambapps.pokemon.alakastats.ui.composables.PokemonTeamPreview
import com.tambapps.pokemon.alakastats.ui.composables.VerticalPokepaste
import com.tambapps.pokemon.alakastats.ui.composables.cardGradientColors
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.gameplan.Composition
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.replay.ReplayCompact
import com.tambapps.pokemon.alakastats.ui.service.FacingDirection
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.util.copyToClipboard
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

// items of the LazyColumn before the game plans: the matchup section and the game plans title
private const val ITEMS_BEFORE_GAME_PLANS = 2

@Composable
fun MatchupPlansEdit(
    team: Teamlytics,
    matchupPlan: MatchupPlan? = null,
    onCancel: () -> Unit,
    onEdited: (MatchupPlan) -> Unit
) {
    val viewModel = koinInject<MatchupPlanEditViewModel>()
    LaunchedEffect(Unit) {
        if (matchupPlan != null) {
            viewModel.prepareEdition(matchupPlan)
        } else {
            viewModel.prepareCreation()
        }
    }
    val isCompact = LocalIsCompact.current
    val scrollState = rememberLazyListState()
    // only scrolls to a game plan the user just added, not to the last one when opening the screen
    var scrollToGamePlan by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(scrollToGamePlan) {
        scrollToGamePlan?.let {
            scrollState.animateScrollToItem(ITEMS_BEFORE_GAME_PLANS + it)
            // so that adding a plan at the same index again (after a deletion) scrolls again
            scrollToGamePlan = null
        }
    }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val onLeave = { if (viewModel.hasChanges) showDiscardDialog = true else onCancel() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (matchupPlan != null) "Edit Matchup" else "Create Matchup") },
                navigationIcon = {
                    BackIconButton(onClick = onLeave)
                }
            )
        },
    ) { scaffoldPadding ->
        Box(
            Modifier.fillMaxSize().padding(scaffoldPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                Modifier
                    .fillMaxHeight()
                    // fields spanning a whole desktop screen are hard to read
                    .widthIn(max = 900.dp)
                    .fillMaxWidth()
                    .padding(
                        if (isCompact) PaddingValues(start = 16.dp, end = 16.dp)
                        else PaddingValues(16.dp)
                    )
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = scrollState,
                ) {
                    item {
                        VerticalSpacer(8.dp)
                        NameInput(viewModel)
                        VerticalSpacer()
                        OpponentTeam(viewModel, team)
                        VerticalSpacer(32.dp)
                    }
                    item {
                        SectionTitle("Game Plans")
                        VerticalSpacer()
                    }

                    itemsIndexed(viewModel.gamePlanStates) { index, gamePlanState ->
                        GamePlanCard(viewModel, team, gamePlanState, index)
                        VerticalSpacer(24.dp)
                    }

                    item {
                        if (viewModel.gamePlanStates.isEmpty()) {
                            Text(
                                "No game plan yet. A game plan tells which Pokémon to bring and how to play them.",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            VerticalSpacer()
                        }
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            OutlinedButton(onClick = {
                                viewModel.createGamePlan()
                                scrollToGamePlan = viewModel.gamePlanStates.lastIndex
                            }) {
                                ButtonIcon(painterResource(Res.drawable.add))
                                Text("Add Game Plan")
                            }
                        }
                        VerticalSpacer(32.dp)
                    }
                }
                ButtonsBar(viewModel, matchupPlan, onLeave, onEdited)
            }
        }
    }
    viewModel.compositionDialogFor?.let { AddToCompositionDialog(viewModel, team, it) }
    viewModel.addReplayDialogFor?.let { SelectReplayExampleDialog(viewModel, team, it) }
    viewModel.gamePlanToDelete?.let { DeleteGamePlanDialog(viewModel, it) }
    if (viewModel.isEditingPokepaste) {
        EditPokepasteDialog(viewModel)
    }
    if (showDiscardDialog) {
        DiscardChangesDialog(onDismiss = { showDiscardDialog = false }, onDiscard = onCancel)
    }
}

@Composable
private fun ButtonsBar(
    viewModel: MatchupPlanEditViewModel,
    matchupPlan: MatchupPlan?,
    onCancel: () -> Unit,
    onEdited: (MatchupPlan) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        viewModel.invalidReason?.let { reason ->
            Text(
                reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = { onEdited(viewModel.generateMatchupPlan(matchupPlan)) },
                modifier = Modifier.weight(1f),
                enabled = viewModel.isFormValid,
            ) {
                Text(if (matchupPlan != null) "Update Matchup" else "Create Matchup")
            }
        }
    }
}

@Composable
private fun OpponentTeam(viewModel: MatchupPlanEditViewModel, team: Teamlytics) {
    var showDetails by remember { mutableStateOf(false) }
    MyCard(gradientBackgroundColors = cardGradientColors, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionSubTitle("Opponent Team")
            VerticalSpacer()
            val pokePaste = viewModel.opponentPokepaste
            if (pokePaste == null) {
                Text(
                    "Add your opponent's PokéPaste to see their team here",
                    style = MaterialTheme.typography.bodyLarge,
                )
                VerticalSpacer()
                OutlinedButton(onClick = viewModel::startPokepasteEdition) {
                    ButtonIcon(painterResource(Res.drawable.content_paste))
                    Text("Add PokéPaste")
                }
            } else {
                PokemonTeamPreview(
                    pokePaste.pokemons.map { it.name },
                    fillWidth = true,
                    facingDirection = FacingDirection.LEFT
                )
                VerticalSpacer()
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { showDetails = true }) {
                        ButtonIcon(painterResource(Res.drawable.visibility))
                        Text("Details")
                    }
                    OutlinedButton(onClick = viewModel::startPokepasteEdition) {
                        ButtonIcon(painterResource(Res.drawable.edit))
                        Text("Edit")
                    }
                }
            }
        }
    }
    val pokePaste = viewModel.opponentPokepaste
    if (showDetails && pokePaste != null) {
        val snackbar = LocalSnackBar.current
        val clipboard = LocalClipboard.current
        val scope = rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text(viewModel.name.ifBlank { "Opponent team" }) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    // vertical on desktop too, as a dialog doesn't take the whole screen width
                    VerticalPokepaste(remember(pokePaste) { pokePaste.asOtsIfNoStats() }, team.format)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetails = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDetails = false
                    scope.launch {
                        val label = "${viewModel.name.ifBlank { "Opponent" }}'s team"
                        if (copyToClipboard(clipboard, label = label, text = pokePaste.toPokePasteString())) {
                            snackbar.show("Copied to clipboard")
                        } else {
                            snackbar.show("Copy to clipboard not supported")
                        }
                    }
                }) {
                    Text("COPY")
                }
            }
        )
    }
}

@Composable
private fun EditPokepasteDialog(viewModel: MatchupPlanEditViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::cancelPokepasteEdition,
        // a click next to the dialog shouldn't lose a paste being typed
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(if (viewModel.opponentPokepaste == null) "Add Opponent Team" else "Edit Opponent Team") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                PokePasteInput(viewModel)
            }
        },
        confirmButton = {
            TextButton(
                onClick = viewModel::applyPokepasteEdition,
                enabled = viewModel.canApplyPokepaste
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::cancelPokepasteEdition) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun GamePlanCard(
    viewModel: MatchupPlanEditViewModel,
    team: Teamlytics,
    gamePlanState: GamePlanState,
    index: Int
) {
    MyCard(gradientBackgroundColors = cardGradientColors, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Game Plan ${index + 1}", modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.gamePlanToDelete = gamePlanState }) {
                    Icon(
                        painter = painterResource(Res.drawable.delete),
                        contentDescription = "Delete Game Plan ${index + 1}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            VerticalSpacer()

            GamePlanComposition(viewModel, gamePlanState)
            VerticalSpacer(24.dp)

            SectionSubTitle("Description")
            VerticalSpacer(8.dp)
            OutlinedTextField(
                value = gamePlanState.description,
                onValueChange = gamePlanState::updateDescription,
                placeholder = { Text("How to play this game plan: leads, win conditions, threats...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                singleLine = false,
            )
            VerticalSpacer(24.dp)
            ReplayExamples(viewModel, team, gamePlanState)
        }
    }
}

@Composable
private fun DeleteGamePlanDialog(viewModel: MatchupPlanEditViewModel, gamePlanState: GamePlanState) {
    val no = viewModel.gamePlanStates.indexOf(gamePlanState) + 1
    AlertDialog(
        onDismissRequest = { viewModel.gamePlanToDelete = null },
        title = { Text("Delete Game Plan $no") },
        text = {
            Text(
                "Are you sure you want to delete this game plan? It will be removed when you save the matchup.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = { viewModel.deleteGamePlan(gamePlanState) }) {
                Text("Delete", color = Color.Red)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.gamePlanToDelete = null }) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DiscardChangesDialog(onDismiss: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Discard Changes") },
        text = {
            Text(
                "This matchup has unsaved changes. Leave without saving them?",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onDiscard()
            }) {
                Text("Discard", color = Color.Red)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Editing")
            }
        }
    )
}

@Composable
private fun SelectReplayExampleDialog(
    viewModel: MatchupPlanEditViewModel,
    team: Teamlytics,
    gamePlanState: GamePlanState
) {
    val selectedReplays = gamePlanState.exampleReplays.toMutableStateList()
    AlertDialog(
        onDismissRequest = { viewModel.addReplayDialogFor = null },
        title = {
            Text("Select Replays")
        },
        text = {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(when(val replaysCount = selectedReplays.size) {
                    0 -> "No replays selected"
                    1 -> "1 replay selected"
                    else -> "$replaysCount replays selected"
                }, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(20.dp))
                val allReplays = remember {
                    // doing this to display selected replays first
                    val (alreadySelected, others) = team.replays.partition { gamePlanState.exampleReplays.contains(it) }
                    alreadySelected + others
                }
                var replays by remember { mutableStateOf(allReplays) }
                var usernameText by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = usernameText,
                    onValueChange = {
                        usernameText = it
                        replays = if (usernameText.isNotBlank()) {
                            team.withContext {
                                allReplays.filter { replay ->
                                    replay.opponentPlayer.name.startsWith(usernameText)
                                }
                            }
                        } else {
                            allReplays
                        }
                    },
                    placeholder = { Text("Filter by Username") },
                    maxLines = 1
                    )
                Spacer(Modifier.height(20.dp))
                LazyColumn(
                    modifier = Modifier
                        .weight(1f),
                    state = rememberLazyListState(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(replays) { replay ->
                        val isSelected = selectedReplays.contains(replay)
                        ReplayCompact(
                            team,
                            replay,
                            onClick = {
                                if (isSelected) {
                                    selectedReplays.remove(replay)
                                } else {
                                    selectedReplays.add(replay)
                                }
                            },
                            gradientBackgroundColors = if (isSelected) cardGradientColors else null,
                            borderColor = if (isSelected) Color.Green else null
                        )
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { selectedReplays.clear() }) {
                Text("Clear")
            }

            TextButton(onClick = { viewModel.addReplayDialogFor = null }) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                gamePlanState.updateExampleReplays(selectedReplays.toList())
                viewModel.addReplayDialogFor = null
            }) {
                Text("Select")
            }
        }
    )

}

@Composable
private fun AddToCompositionDialog(
    viewModel: MatchupPlanEditViewModel,
    team: Teamlytics,
    gamePlanState: GamePlanState
) {
    val pokemons = team.pokePaste.pokemons.map { it.name }
    val selectedPokemons =
        remember { gamePlanState.composition.filter { pokemons.contains(it) }.toMutableStateList() }

    AlertDialog(
        onDismissRequest = { viewModel.compositionDialogFor = null },
        title = {
            val no = remember(gamePlanState) { viewModel.gamePlanStates.indexOf(gamePlanState) + 1 }
            Text("Game Plan $no Composition")
        },
        text = {
            Column {
                Text(
                    "Select the Pokemons to bring into battle",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "The first 2 selected are the leads, the others are the back (up to 5 Pokémon)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    pokemons.forEach { pokemonName ->
                        val isSelected = selectedPokemons.contains(pokemonName)
                        PokemonFilterChip(
                            pokemonName = pokemonName,
                            onClick = {
                                when {
                                    // 5 and not 4 because I allow the user to have many back pokemon options
                                    selectedPokemons.size < 5 && !isSelected -> selectedPokemons.add(
                                        pokemonName
                                    )

                                    isSelected -> selectedPokemons.remove(pokemonName)
                                }
                            },
                            asLead = isSelected && selectedPokemons.indexOf(pokemonName) <= 1,
                            selected = isSelected
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { selectedPokemons.clear() }) {
                Text("Clear")
            }

            TextButton(onClick = { viewModel.compositionDialogFor = null }) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                gamePlanState.updateComposition(selectedPokemons)
                viewModel.compositionDialogFor = null
            }) {
                Text("Select")
            }
        }
    )
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) = Text(
    text = text,
    style = MaterialTheme.typography.headlineSmall,
    fontWeight = FontWeight.Bold,
    modifier = modifier
)

@Composable
private fun SectionSubTitle(text: String, modifier: Modifier = Modifier) = Text(
    text = text,
    style = MaterialTheme.typography.titleLarge,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    modifier = modifier
)

@Composable
private fun ButtonIcon(painter: Painter) {
    Icon(painter = painter, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
    Spacer(Modifier.width(8.dp))
}

@Composable
private fun NameInput(viewModel: MatchupPlanEditViewModel) {
    OutlinedTextField(
        value = viewModel.name,
        onValueChange = viewModel::updateName,
        label = { Text("Matchup Name") },
        placeholder = { Text("e.g. Sun, Trick Room, Kyogre...") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
private fun GamePlanComposition(
    viewModel: MatchupPlanEditViewModel,
    gamePlanState: GamePlanState
) {
    val composition = gamePlanState.composition
    Row(verticalAlignment = Alignment.CenterVertically) {
        // the title takes what the button leaves, so that both fit on a phone
        SectionSubTitle(text = "Composition", modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        OutlinedButton(onClick = { viewModel.compositionDialogFor = gamePlanState }) {
            ButtonIcon(painterResource(Res.drawable.edit))
            Text("Choose Pokémon")
        }
    }
    VerticalSpacer(8.dp)

    if (composition.isEmpty()) {
        Text("No Pokémon chosen yet", style = MaterialTheme.typography.bodyLarge)
    } else {
        // as the game plan will be shown in the tab
        Composition(
            composition,
            pokemonSize = if (LocalIsCompact.current) 64.dp else 96.dp,
            facingDirection = FacingDirection.RIGHT,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ReplayExamples(
    viewModel: MatchupPlanEditViewModel,
    team: Teamlytics,
    gamePlanState: GamePlanState
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionSubTitle(text = "Example Replays", modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        OutlinedButton(onClick = { viewModel.addReplayDialogFor = gamePlanState }) {
            ButtonIcon(painterResource(Res.drawable.edit))
            Text("Choose Replays")
        }
    }
    VerticalSpacer(8.dp)
    if (gamePlanState.exampleReplays.isEmpty()) {
        Text("No example replay", style = MaterialTheme.typography.bodyLarge)
    }
    gamePlanState.exampleReplays.forEach { replay ->
        ReplayCompact(team, replay)
        VerticalSpacer()
    }
}

@Composable
private fun VerticalSpacer(height: Dp = 16.dp) = Spacer(Modifier.height(height))
