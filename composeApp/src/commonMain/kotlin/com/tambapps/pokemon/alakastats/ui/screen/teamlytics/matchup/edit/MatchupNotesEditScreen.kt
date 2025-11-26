@file:OptIn(ExperimentalMaterial3Api::class)

package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.matchup.edit

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.add
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import arrow.core.Either
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.domain.model.MatchupNotes
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton
import com.tambapps.pokemon.alakastats.ui.composables.PokePasteInput
import com.tambapps.pokemon.alakastats.ui.composables.PokemonFilterChip
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.theme.defaultIconColor
import org.jetbrains.compose.resources.painterResource

class MatchupNotesEditScreen(
    private val team: Teamlytics,
    private val onSuccess: suspend (MatchupNotes) -> Either<DomainError, Unit>,
    private val matchupNotes: MatchupNotes? = null
) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<MatchupNotesEditViewModel>()
        LaunchedEffect(Unit) {
            if (matchupNotes != null) {
                viewModel.prepareEdition(matchupNotes)
            }
        }
        val isCompact = LocalIsCompact.current
        val navigator = LocalNavigator.currentOrThrow
        val scrollState = rememberScrollState()

        LaunchedEffect(viewModel.gamePlanStates.size) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (matchupNotes != null) "Edit Matchup" else "Create Matchup") },
                    navigationIcon = {
                        BackIconButton(navigator)
                    }
                )
            }
        ) { scaffoldPadding ->
            val paddingValues = if (isCompact) PaddingValues(start = 16.dp, end = 16.dp)
            else PaddingValues(16.dp)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .padding(paddingValues)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                SectionTitle("Matchup Name")

                NameInput(viewModel)

                PokePasteInput(viewModel)

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Game Plans")
                    Spacer(Modifier.width(16.dp))
                    AddButton(
                        onClick = { viewModel.createGamePlan() },
                        contentDescription = "Add Game Plan"
                    )
                }

                viewModel.gamePlanStates.forEachIndexed { index, gamePlanState ->
                    SectionTitle("Game Plan ${index + 1}")
                    OutlinedTextField(
                        value = gamePlanState.description,
                        onValueChange = gamePlanState::updateDescription,
                        placeholder = { Text("Type your game plan") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp),
                        singleLine = false,
                    )

                    GamePlanComposition(viewModel, gamePlanState)

                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(16.dp))
            }
        }
        viewModel.compositionDialogFor?.let { AddToCompositionDialog(viewModel, team, it) }
    }
}

@Composable
private fun AddToCompositionDialog(
    viewModel: MatchupNotesEditViewModel,
    team: Teamlytics,
    gamePlanState: GamePlanState
) {
    val index = remember(gamePlanState) { viewModel.gamePlanStates.indexOf(gamePlanState) + 1 }
    val pokemons = team.pokePaste.pokemons.map { it.name }
    val selectedPokemons =
        remember { gamePlanState.composition.filter { pokemons.contains(it) }.toMutableStateList() }

    AlertDialog(
        onDismissRequest = { viewModel.compositionDialogFor = null },
        title = { Text("Game Plan $index Composition") },
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
                                    selectedPokemons.size < 4 && !isSelected -> selectedPokemons.add(
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
private fun SectionTitle(text: String) = Text(
    text = text,
    style = MaterialTheme.typography.headlineSmall,
    fontWeight = FontWeight.Bold
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
private fun AddButton(onClick: () -> Unit, contentDescription: String?) {
    OutlinedButton(
        onClick = onClick,
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
    ) {
        Icon(
            painter = painterResource(Res.drawable.add),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.defaultIconColor
        )
    }
}

@Composable
private fun GamePlanComposition(
    viewModel: MatchupNotesEditViewModel,
    gamePlanState: GamePlanState
) {
    val composition = gamePlanState.composition
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Composition",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(16.dp))
        AddButton(
            onClick = { viewModel.compositionDialogFor = gamePlanState },
            contentDescription = "Add Pokemon to Composition"
        )
    }
    if (composition.isEmpty()) {
        Text("Which pokemon to bring into battle")
    } else {
        val isCompact = LocalIsCompact.current
        Row(Modifier.fillMaxWidth()) {
            composition.forEach { pokemonName ->
                viewModel.pokemonImageService.PokemonSprite(pokemonName,
                    modifier = Modifier.then(if (isCompact) Modifier.weight(1f) else Modifier.size(64.dp))
                        .scale(scaleX = -1f, scaleY = 1f))
            }
        }
    }
}