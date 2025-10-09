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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.composables.LinearProgressBarIfEnabled
import com.tambapps.pokemon.alakastats.ui.composables.Pokepaste
import kotlin.collections.chunked
import kotlin.collections.forEachIndexed


@Composable
internal fun OverviewTabDesktop(viewModel: OverviewViewModel) {
    val team = viewModel.team
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        LinearProgressBarIfEnabled(viewModel.isLoading)
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeamName(team, modifier = Modifier.weight(1f))

            if (viewModel.isEditingNotes) {
                NoteEditingButtons(viewModel)
            } else {
                MoreActionsButton(viewModel)
            }
            Spacer(Modifier.width(8.dp))
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
    }
}

@Composable
private fun NotedPokePaste(viewModel: OverviewViewModel) {
    val pokemonBlocks = remember { viewModel.team.pokePaste.pokemons.chunked(2) }
    for (pokemons in pokemonBlocks) {
        Row(Modifier.fillMaxWidth()) {
            pokemons.forEachIndexed { index, pokemon ->
                if (index > 0) {
                    Spacer(Modifier.width(16.dp))
                }
                PokemonNotes(
                    viewModel, pokemon,
                    pokepasteModifier = Modifier.fillMaxWidth().height(256.dp),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}