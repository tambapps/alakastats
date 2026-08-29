package com.tambapps.pokemon.alakastats.ui.screen.manualreplay

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.composables.verticalPokemonSpace
import com.tambapps.pokemon.alakastats.ui.theme.teamlyticsTabPaddingBottom

private const val POKEMONS_PER_ROW = 3

@Composable
internal fun ManualReplayScreenDesktop(viewModel: ManualReplayViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        for (pokemonBlock in viewModel.pokemonStates.chunked(POKEMONS_PER_ROW)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = verticalPokemonSpace)
            ) {
                pokemonBlock.forEachIndexed { index, pokemonState ->
                    if (index > 0) {
                        Spacer(Modifier.width(16.dp))
                    }
                    ManualPokemonCard(viewModel, pokemonState, Modifier.weight(1f))
                }
                // keep cards of incomplete rows the same width as the ones of full rows
                repeat(POKEMONS_PER_ROW - pokemonBlock.size) {
                    Spacer(Modifier.width(16.dp))
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(teamlyticsTabPaddingBottom))
    }
}
