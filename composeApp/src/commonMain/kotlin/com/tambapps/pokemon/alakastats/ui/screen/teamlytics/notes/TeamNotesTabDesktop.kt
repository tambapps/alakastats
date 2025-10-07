package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.notes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun TeamNotesTabDesktop(viewModel: TeamNotesViewModel) {
    val team = viewModel.team
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        TeamName(team)
        Spacer(Modifier.height(16.dp))
        NoteTextOrTextField(viewModel, viewModel.teamNotes, "Team notes") {
            viewModel.teamNotes = it
        }
        Spacer(Modifier.height(16.dp))

        val entryBlocks = remember { viewModel.pokemonNotes.entries.chunked(2) }
        for (entryBlock in entryBlocks) {
            Row(Modifier.fillMaxWidth()) {
                entryBlock.forEachIndexed { index, (pokemonName, notes) ->
                    if (index > 0) {
                        Spacer(Modifier.width(16.dp))
                    }
                    PokemonNotes(viewModel, pokemonName, notes,
                        artworkModifier = Modifier.size(256.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
