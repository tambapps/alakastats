package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.overview

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.more_vert
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.NbReplaysText
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.WinRateText
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
    if (team.replays.isEmpty()) {
        NbReplaysText(
            team = team,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        return
    }
    Row(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Spacer(Modifier.weight(1f))
        NbReplaysText(team)
        Spacer(Modifier.width(32.dp))
        val winRate = team.computeWinRatePercentage()
        WinRateText(winRate)
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
internal fun NoteEditingButtons(viewModel: OverviewViewModel) {
    val snackBar = LocalSnackBar.current
    Button(
        onClick = { viewModel.saveNotes(snackBar) }
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


    OutlinedIconButton(
        onClick = { isMenuExpanded = !isMenuExpanded },
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.defaultIconColor)
    ) {
        Icon(
            modifier = Modifier.size(40.dp),
            painter = painterResource(Res.drawable.more_vert),
            contentDescription = "More",
            tint = MaterialTheme.colorScheme.defaultIconColor
        )
        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false }
        ) {
            val snackBar = LocalSnackBar.current
            DropdownMenuItem(
                text = { Text("Export") },
                onClick = {
                    isMenuExpanded = false
                    viewModel.exportTeam(snackBar)
                }
            )
            val navigator = LocalNavigator.currentOrThrow
            DropdownMenuItem(
                text = { Text("Edit team") },
                onClick = {
                    isMenuExpanded = false
                    navigator.push(EditTeamScreen(viewModel.team))
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
internal fun NotedPokepastePokemon(viewModel: OverviewViewModel,
                          pokemon: Pokemon,
                          modifier: Modifier = Modifier,
                          ) {
    val notes = viewModel.pokemonNotes[pokemon]
    if (viewModel.isEditingNotes) {
        PokepastePokemon(
            viewModel.team.pokePaste.isOts,
            pokemon,
            viewModel.pokemonImageService,
            modifier,
            onNotesChanged = { viewModel.pokemonNotes[pokemon] = it },
            notes
        )
    } else {
        PokepastePokemon(
            viewModel.team.pokePaste.isOts,
            pokemon,
            viewModel.pokemonImageService,
            modifier,
            notes
        )
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
    } else if (notes.isNotBlank()) {
        Text(notes, style = noteTextFieldTextStyle)
    }
}

