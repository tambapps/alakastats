package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.composables.PokepastePokemonHeader
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.usage.PokemonUsagesCard

@Composable
fun PokemonDetailsDesktop(
    viewModel: PokemonDetailViewModel,
    state: TeamPokemonStateState.Loaded
) {
    val (team, pokemon, pokemonData, notes, usages) = state
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        PokepastePokemonHeader(state.pokemon, viewModel.pokemonImageService)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) {
            PokemonDetailsOverview(viewModel, state, Modifier.weight(1f))
            Column(Modifier.weight(1f)) {

                usages?.let {
                    PokemonUsagesCard(
                        viewModel.pokemonImageService,
                        team.replays,
                        pokemon.name,
                        it,
                    )
                }
            }
        }
    }
}