package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.notes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
internal fun TeamNotesTabMobile(viewModel: TeamNotesViewModel) {
    val team = viewModel.team
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        TeamName(team)
        Spacer(Modifier.height(16.dp))
        NoteTextOrTextField(viewModel, viewModel.teamNotes, "Team notes") { viewModel.teamNotes = it }
        Spacer(Modifier.height(16.dp))
        PokemonsTitle()
        Spacer(Modifier.height(16.dp))
        for ((pokemonName, notes) in viewModel.pokemonNotes) {
            PokemonNotes(viewModel, pokemonName, notes)
        }
    }
}
