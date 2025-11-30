package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.matchup

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.add
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.MatchupNotes
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.SnackBar
import com.tambapps.pokemon.alakastats.ui.composables.ExpansionTile
import com.tambapps.pokemon.alakastats.ui.composables.FabLayout
import com.tambapps.pokemon.alakastats.ui.composables.PokemonTeamPreview
import com.tambapps.pokemon.alakastats.ui.composables.cardGradientColors
import com.tambapps.pokemon.alakastats.ui.screen.home.buttonTextStyle
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.matchup.edit.MatchupNotesEdit
import com.tambapps.pokemon.alakastats.ui.service.FacingDirection
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import org.jetbrains.compose.resources.painterResource


@Composable
fun MatchupNotesTab(viewModel: MatchupNotesViewModel) {
    val snackBar = LocalSnackBar.current
    when (val mode = viewModel.editMatchupMode) {
        NoEdit -> {
            FabLayout(
                fab = {
                    if (viewModel.hasMatchupNotes) {
                        FloatingActionButton(onClick = { viewModel.editMatchupMode = CreateMatchup },) {
                            Icon(
                                painter = painterResource(Res.drawable.add),
                                contentDescription = "Add",
                            )
                        }
                    }
                }
            ) {
                if (!viewModel.hasMatchupNotes) {
                    NoNotes(viewModel)
                } else if (LocalIsCompact.current)  {
                    MatchupNotesTabMobile(viewModel)
                } else {
                    MatchupNotesTabDesktop(viewModel)
                }
            }
        }
        CreateMatchup, is EditMatchup -> MatchupNotesEdit(
            team = viewModel.team,
            matchupNotes = (mode as? EditMatchup)?.matchupNotes,
            onCancel = { viewModel.editMatchupMode = NoEdit },
            onEdited = { editedMatchup ->
                val verb = if (mode == CreateMatchup) "created" else "edited"
                viewModel.saveMatchup(
                    editedMatchup,
                    onSuccess = { snackBar.show("Successfully $verb matchup", SnackBar.Severity.SUCCESS) },
                    onError = { snackBar.show("Error: ${it.message}", SnackBar.Severity.ERROR) }
                )
            }
        )
    }
}

@Composable
fun NoNotes(viewModel: MatchupNotesViewModel) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.align(Alignment.Center).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Add your game plans for each matchup to remember how to play against the meta",
                style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))

            Button(onClick = { viewModel.editMatchupMode = CreateMatchup }) {
                Icon(
                    painter = painterResource(Res.drawable.add),
                    contentDescription = "Add",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.width(8.dp))
                Text("Create Matchup", style = buttonTextStyle.copy(
                    color = LocalContentColor.current
                ))
            }
        }
    }
}

@Composable
internal fun MatchNotes(
    viewModel: MatchupNotesViewModel,
    matchupNotes: MatchupNotes,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    ExpansionTile(
        modifier = modifier,
        gradientBackgroundColors = cardGradientColors,
        title = {
            Text(
                text = matchupNotes.name,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        subtitle = {
            matchupNotes.pokePaste?.let { pokePaste ->
                PokemonTeamPreview(
                    viewModel.pokemonImageService,
                    pokePaste.pokemons.map { it.name },
                    fillWidth = true
                )
            }
        },
        menu = { isMenuExpandedState ->
            DropdownMenu(
                expanded = isMenuExpandedState.value,
                onDismissRequest = { isMenuExpandedState.value = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = { viewModel.editMatchupMode = EditMatchup(matchupNotes) }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { showDeleteDialog = true }
                )
            }
        },
        content = content
    )
    if (showDeleteDialog) {
        DeleteMatchupDialog(viewModel, matchupNotes, onDismiss = { showDeleteDialog = false })
    }
}

@Composable
internal fun Composition(composition: List<PokemonName>, pokemonImageService: PokemonImageService, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        composition.chunked(2).forEachIndexed { index, chunk ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(50.dp)
                    )
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(if (index == 0) "Lead" else "Back", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(16.dp))
                chunk.forEach { pokemonName ->
                    pokemonImageService.PokemonArtwork(pokemonName, Modifier.size(80.dp).padding(horizontal = 4.dp)
                        , facingDirection = FacingDirection.RIGHT)
                }
            }
            if (index == 0 && composition.size > 1) {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
internal fun DeleteMatchupDialog(viewModel: MatchupNotesViewModel, matchupNotes: MatchupNotes, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Matchup ${matchupNotes.name}") },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to delete this matchup?",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        },
        confirmButton = {
            val snackBar = LocalSnackBar.current
            TextButton(
                onClick = {
                    viewModel.deleteMatchup(matchupNotes,
                        onSuccess = { snackBar.show("Successfully deleted matchup", SnackBar.Severity.SUCCESS) },
                        onError = { snackBar.show("Error: ${it.message}", SnackBar.Severity.ERROR) })
                    onDismiss.invoke()
                },
            ) {
                Text("Delete", color = Color.Red)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}