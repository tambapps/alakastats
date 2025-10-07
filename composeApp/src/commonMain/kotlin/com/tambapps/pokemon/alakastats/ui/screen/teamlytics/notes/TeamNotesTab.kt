package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.notes

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.add
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.ui.screen.home.buttonTextStyle
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import org.jetbrains.compose.resources.painterResource

@Composable
fun TeamNotesTab(viewModel: TeamNotesViewModel) {
    val isCompact = LocalIsCompact.current
    if (!viewModel.isEditing) {
        NoNotes(viewModel)
    } else if (isCompact) {
        TeamNotesTabMobile(viewModel)
    } else {
        TeamNotesTabDesktop(viewModel)
    }
}

@Composable
private fun NoNotes(viewModel: TeamNotesViewModel) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("No notes were found")
            AddNotesButton(viewModel)
        }
    }
}


@Composable
internal fun PokemonNotes(viewModel: TeamNotesViewModel, pokemonName: PokemonName,
                          notes: String,
                          artworkModifier: Modifier = Modifier,
                          modifier: Modifier = Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(pokemonName.pretty, style = MaterialTheme.typography.headlineMedium)
        viewModel.pokemonImageService.PokemonArtwork(pokemonName, artworkModifier)
        NoteTextOrTextField(
            viewModel = viewModel,
            notes = notes,
            onValueChange = { viewModel.pokemonNotes[pokemonName] = it },
            placeholder = "Notes for ${pokemonName.pretty}"
        )
    }
}

private val textFieldTextStyle
    @Composable
    get() = MaterialTheme.typography.bodyMedium

@Composable
internal fun NoteTextOrTextField(
    viewModel: TeamNotesViewModel,
    notes: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    if (viewModel.isEditing) {
        OutlinedTextField(
            value = notes,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = textFieldTextStyle
        )
    } else {
        Text(notes, style = textFieldTextStyle)
    }
}

@Composable
internal fun AddNotesButton(viewModel: TeamNotesViewModel) {
    Button(
        onClick = { viewModel.editMode(true) }
    ) {
        Icon(
            painter = painterResource(Res.drawable.add),
            contentDescription = "Add",
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.width(8.dp))
        Text("Add Notes", style = buttonTextStyle.copy(
            color = LocalContentColor.current
        ))
    }
}

@Composable
fun TeamName(team: Teamlytics) {
    Text(
        text = team.name,
        style = MaterialTheme.typography.displayLarge,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun PokemonsTitle() {
    Text(
        text = "Pokemons",
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold
    )
}

