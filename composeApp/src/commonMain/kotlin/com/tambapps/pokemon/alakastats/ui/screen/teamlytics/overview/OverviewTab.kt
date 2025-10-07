package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.overview

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.add
import alakastats.composeapp.generated.resources.more_vert
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.computeWinRatePercentage
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.composables.PokepastePokemon
import com.tambapps.pokemon.alakastats.ui.screen.editteam.EditTeamScreen
import com.tambapps.pokemon.alakastats.ui.screen.home.buttonTextStyle
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.theme.defaultIconColor
import org.jetbrains.compose.resources.painterResource
import kotlin.collections.set

@Composable
fun OverviewTab(viewModel: OverviewViewModel) {
    val isCompact = LocalIsCompact.current
    if (isCompact) {
        OverviewTabMobile(viewModel)
    } else {
        OverviewTabDesktop(viewModel)
    }
}

@Composable
internal fun TeamName(team: Teamlytics, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = team.name,
        style = MaterialTheme.typography.displayLarge,
        fontWeight = FontWeight.Bold
    )
}

@Composable
internal fun Header(team: Teamlytics) {
    val replaysCount = remember { team.replays.size }
    val textStyle = MaterialTheme.typography.titleLarge
    if (replaysCount == 0) {
        Text(
            "${team.replays.size} replays",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )
        return
    }
    Row(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Spacer(Modifier.weight(1f))
        Text("$replaysCount replays", style = textStyle)
        Spacer(Modifier.width(32.dp))
        val winRate = remember { team.computeWinRatePercentage() }
        Text("$winRate% winrate", style = textStyle)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
internal fun PokePasteTitle() {
    Text(
        text = "PokePaste",
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold
    )
}

@Composable
internal fun EditButton(team: Teamlytics) {
    val navigator = LocalNavigator.currentOrThrow

    OutlinedButton(onClick = { navigator.push(EditTeamScreen(team))}) {
        Text("Edit")
    }
}

@Composable
internal fun NoteEditingButtons(viewModel: OverviewViewModel) {
    Button(
        onClick = { viewModel.saveNotes() }
    ) {
        Text("Save Notes", style = buttonTextStyle.copy(
            color = LocalContentColor.current
        ))
    }
    Spacer(Modifier.width(8.dp))
    OutlinedButton(onClick = { viewModel.cancelEditingNotes() }) {
        Text("Cancel")
    }
}

@Composable
internal fun MoreActionsButton(viewModel: OverviewViewModel) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val snackbar = LocalSnackBar.current

    IconButton(onClick = { isMenuExpanded = !isMenuExpanded }) {
        Icon(
            modifier = if (LocalIsCompact.current) Modifier else Modifier.size(40.dp),
            painter = painterResource(Res.drawable.more_vert),
            contentDescription = "More",
            tint = MaterialTheme.colorScheme.defaultIconColor
        )
        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Export") },
                onClick = {
                    isMenuExpanded = false
                    snackbar.show("TODO (not implemented yet)")
                }
            )

            val alreadyHasNotes = viewModel.team.notes != null
            DropdownMenuItem(
                text = { Text(
                    if (!alreadyHasNotes) "Add notes" else "Edit notes"
                ) },
                onClick = {
                    viewModel.editNotes()
                    isMenuExpanded = false
                }
            )

            if (alreadyHasNotes) {
                DropdownMenuItem(
                    text = { Text("Remove notes") },
                    onClick = {
                        viewModel.removeNotes()
                        isMenuExpanded = false
                    }
                )
            }

        }
    }
}

@Composable
internal fun PokemonNotes(viewModel: OverviewViewModel, pokemon: Pokemon,
                          notes: String,
                          modifier: Modifier = Modifier,
                          pokepasteModifier: Modifier = Modifier
                          ) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(pokemon.name.pretty, style = MaterialTheme.typography.headlineMedium)
        PokepastePokemon(viewModel.team.pokePaste.isOts, pokemon, viewModel.pokemonImageService, pokepasteModifier)

        Spacer(Modifier.height(8.dp))
        NoteTextOrTextField(
            viewModel = viewModel,
            notes = notes,
            onValueChange = { viewModel.pokemonNotes[pokemon] = it },
            placeholder = "Notes for ${pokemon.name.pretty}"
        )
        Spacer(Modifier.height(16.dp))
    }
}

private val noteTextFieldTextStyle
    @Composable
    get() = MaterialTheme.typography.bodyLarge

@Composable
internal fun NoteTextOrTextField(
    viewModel: OverviewViewModel,
    notes: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    if (viewModel.isEditingNotes) {
        OutlinedTextField(
            value = notes,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = noteTextFieldTextStyle
        )
    } else {
        Text(notes, style = noteTextFieldTextStyle)
    }
}

