package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.overview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.composables.LinearProgressBarIfEnabled
import com.tambapps.pokemon.alakastats.ui.composables.Pokepaste
import kotlin.collections.component1
import kotlin.collections.component2

@Composable
internal fun OverviewTabMobile(viewModel: OverviewViewModel) {
    val team = viewModel.team
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (viewModel.isEditingNotes) {
                Row(Modifier.align(Alignment.CenterHorizontally)) {
                    NoteEditingButtons(viewModel)
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                TeamName(team, modifier = Modifier.weight(1f))

                if (!viewModel.isEditingNotes) {
                    MoreActionsButton(viewModel)
                }
                Spacer(Modifier.width(4.dp))
            }
            Spacer(Modifier.height(4.dp))
            NoteTextOrTextField(viewModel, viewModel.teamNotes, "Team notes") {
                viewModel.teamNotes = it
            }
            Spacer(Modifier.height(8.dp))
            Header(team)

            PokePasteTitle()
            Spacer(Modifier.height(16.dp))
            if (viewModel.isEditingNotes || team.notes != null) {
                NotedPokePaste(viewModel)
            } else {
                Pokepaste(team.pokePaste, viewModel.pokemonImageService)
            }
            Spacer(Modifier.height(12.dp))
        }
        LinearProgressBarIfEnabled(viewModel.isLoading)
    }
}

@Composable
private fun NotedPokePaste(viewModel: OverviewViewModel) {
    val entries = viewModel.pokemonNotes.entries
    entries.forEachIndexed { index, (pokemonName, notes) ->
        PokemonNotes(
            viewModel, pokemonName, notes,
            pokepasteModifier = Modifier.fillMaxWidth().height(256.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}