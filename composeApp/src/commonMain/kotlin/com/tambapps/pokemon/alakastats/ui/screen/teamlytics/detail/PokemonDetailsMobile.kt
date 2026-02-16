package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail

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
import com.tambapps.pokemon.alakastats.ui.composables.PokepastePokemonHeader

@Composable
fun PokemonDetailsMobile(
    viewModel: PokemonDetailViewModel,
    state: TeamPokemonStateState.Loaded
) {
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .then(Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
    ){
        PokepastePokemonHeader(state.pokemon, viewModel.pokemonImageService)
        Spacer(Modifier.height(16.dp))

        PokemonDetailsOverview(viewModel, state, Modifier.fillMaxSize())
    }
}