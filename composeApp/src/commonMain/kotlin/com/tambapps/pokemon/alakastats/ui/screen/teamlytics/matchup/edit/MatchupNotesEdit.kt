@file:OptIn(ExperimentalMaterial3Api::class)

package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.matchup.edit

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.add
import alakastats.composeapp.generated.resources.edit
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tambapps.pokemon.alakastats.domain.model.MatchupNotes
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton
import com.tambapps.pokemon.alakastats.ui.composables.PokePasteInput
import com.tambapps.pokemon.alakastats.ui.composables.PokemonFilterChip
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.replay.ReplayCompact
import com.tambapps.pokemon.alakastats.ui.service.FacingDirection
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.theme.defaultIconColor
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

@Composable
fun MatchupNotesEdit(
    team: Teamlytics,
    matchupNotes: MatchupNotes? = null,
    onCancel: () -> Unit,
    onEdited: (MatchupNotes) -> Unit
) {
    val viewModel = koinInject<MatchupNotesEditViewModel>()
    LaunchedEffect(Unit) {
        if (matchupNotes != null) {
            viewModel.prepareEdition(matchupNotes)
        }
    }
    val isCompact = LocalIsCompact.current
    val scrollState = rememberLazyListState()

    LaunchedEffect(viewModel.gamePlanStates.size) {
        val lastIndex =
            1 + viewModel.gamePlanStates.size // 1 because we created one item before the gamePlanStates
        scrollState.animateScrollToItem(lastIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (matchupNotes != null) "Edit Matchup" else "Create Matchup") },
                navigationIcon = {
                    BackIconButton(onCancel)
                }
            )
        },
    ) { scaffoldPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
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
                    SectionTitle("Matchup Name")
                    VerticalSpacer()

                    NameInput(viewModel)
                    VerticalSpacer()

                    PokePasteInput(viewModel)
                    VerticalSpacer(24.dp)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("Game Plans")
                        Spacer(Modifier.width(16.dp))
                        OutlinedIconButton(onClick = { viewModel.createGamePlan() }) {
                            Icon(
                                painter = painterResource(Res.drawable.add),
                                contentDescription = "Add New Game Plan",
                                tint = MaterialTheme.colorScheme.defaultIconColor
                            )
                        }
                    }
                }

                itemsIndexed(viewModel.gamePlanStates) { index, gamePlanState ->
                    SectionTitle("Game Plan ${index + 1}", fontSize = 23.sp)
                    VerticalSpacer()

                    GamePlanComposition(viewModel, gamePlanState)
                    VerticalSpacer()

                    SectionSubTitle("Description")
                    VerticalSpacer()
                    OutlinedTextField(
                        value = gamePlanState.description,
                        onValueChange = gamePlanState::updateDescription,
                        placeholder = { Text("Type your game plan") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp),
                        singleLine = false,
                    )
                    VerticalSpacer()
                    ReplayExamples(viewModel, gamePlanState)
                    VerticalSpacer(32.dp)
                }

                item {
                    VerticalSpacer(32.dp)
                }
            }
            ButtonsBar(viewModel, matchupNotes, onCancel, onEdited)
        }
    }
    viewModel.compositionDialogFor?.let { AddToCompositionDialog(viewModel, team, it) }
    viewModel.addReplayDialogFor?.let { AddReplayExampleDialog(viewModel, team, it) }
}

@Composable
private fun ButtonsBar(
    viewModel: MatchupNotesEditViewModel,
    matchupNotes: MatchupNotes?,
    onCancel: () -> Unit,
    onEdited: (MatchupNotes) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f)
        ) {
            Text("Cancel")
        }

        Button(
            onClick = { onEdited(viewModel.generateMatchupNotes()) },
            modifier = Modifier.weight(1f),
            enabled = viewModel.isFormValid,
        ) {
            Text(
                if (matchupNotes != null) "Update Matchup" else "Create Matchup",
                // important
                color = LocalContentColor.current
            )
        }
    }
}

@Composable
private fun AddReplayExampleDialog(
    viewModel: MatchupNotesEditViewModel,
    team: Teamlytics,
    gamePlanState: GamePlanState
) {
    val selectedReplays = mutableStateListOf<ReplayAnalytics>()
    AlertDialog(
        onDismissRequest = { viewModel.compositionDialogFor = null },
        title = {
            Text("Pick Replays")
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
                LazyColumn(
                    modifier = Modifier
                        .weight(1f),
                    state = rememberLazyListState(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(team.replays) { replay ->
                        ReplayCompact(team, replay, viewModel.pokemonImageService)
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { selectedReplays.clear() }) {
                Text("Clear")
            }

            TextButton(onClick = { viewModel.compositionDialogFor = null }) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // TODO update replaysgamePlanState.updateComposition(selectedPokemons)
                viewModel.compositionDialogFor = null
            }) {
                Text("Select")
            }
        }
    )

}

@Composable
private fun AddToCompositionDialog(
    viewModel: MatchupNotesEditViewModel,
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
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    pokemons.forEach { pokemonName ->
                        val isSelected = selectedPokemons.contains(pokemonName)
                        PokemonFilterChip(
                            pokemonName = pokemonName,
                            pokemonImageService = viewModel.pokemonImageService,
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
private fun SectionTitle(text: String, fontSize: TextUnit = TextUnit.Unspecified) = Text(
    text = text,
    style = MaterialTheme.typography.headlineSmall,
    fontWeight = FontWeight.Bold,
    fontSize = fontSize
)

@Composable
private fun SectionSubTitle(text: String) = Text(
    text = text,
    style = MaterialTheme.typography.titleLarge,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp
)


@Composable
private fun NameInput(viewModel: MatchupNotesEditViewModel) {
    OutlinedTextField(
        value = viewModel.name,
        onValueChange = viewModel::updateName,
        placeholder = { Text("Enter a name for your matchup") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
private fun GamePlanComposition(
    viewModel: MatchupNotesEditViewModel,
    gamePlanState: GamePlanState
) {
    val composition = gamePlanState.composition
    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionSubTitle(text = "Composition")
        Spacer(Modifier.width(16.dp))
        OutlinedIconButton(
            onClick = { viewModel.compositionDialogFor = gamePlanState },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.edit),
                modifier = Modifier.size(16.dp),
                contentDescription = "Add Pokemon to Composition",
                tint = MaterialTheme.colorScheme.defaultIconColor
            )
        }
    }

    if (composition.isEmpty()) {
        Spacer(Modifier.height(16.dp))
        Text("Select pokemons to bring into battle")
    } else {
        val isCompact = LocalIsCompact.current
        Row(Modifier.fillMaxWidth()) {
            composition.forEach { pokemonName ->
                viewModel.pokemonImageService.PokemonSprite(
                    pokemonName,
                    modifier = Modifier.then(
                        if (isCompact) Modifier.weight(1f) else Modifier.size(
                            64.dp
                        )
                    )
                )
            }
        }
    }
}

@Composable
private fun ReplayExamples(
    viewModel: MatchupNotesEditViewModel,
    gamePlanState: GamePlanState
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionSubTitle(text = "Replay examples")
        Spacer(Modifier.width(16.dp))
        OutlinedIconButton(
            onClick = { viewModel.addReplayDialogFor = gamePlanState },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.add),
                modifier = Modifier.size(16.dp),
                contentDescription = "Add Replay",
                tint = MaterialTheme.colorScheme.defaultIconColor
            )
        }
    }

}
@Composable
private fun VerticalSpacer(height: Dp = 16.dp) = Spacer(Modifier.height(height))