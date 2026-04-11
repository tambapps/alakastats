package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail.tabs.overview

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.composables.MegaSwitch
import com.tambapps.pokemon.alakastats.ui.composables.PokepastePokemonHeader
import com.tambapps.pokemon.util.MegaUtils

@Composable
fun PokemonDetailsOverviewMobile(
    viewModel: PokemonDetailOverviewModel,
    scrollState: ScrollState,
) {
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(scrollState)
            .then(Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
    ){

        val pokemon = remember(viewModel.megaPokemon, viewModel.megaSelected) { if (viewModel.megaPokemon != null && viewModel.megaSelected) viewModel.pokemon.copy(name = viewModel.megaPokemon) else viewModel.pokemon }

        PokepastePokemonHeader(
            pokemon = pokemon,
            pokemonImageService = viewModel.pokemonImageService,
            format = viewModel.team.format,
            megaSwitch = if (viewModel.megaPokemon != null) ({ MegaSwitch(viewModel.megaSelected, onCheckedChange = { viewModel.megaSelected = it }) })
            else null
        )
        Spacer(Modifier.height(16.dp))

        PokemonDetailsOverview(viewModel, pokemon=pokemon, Modifier.fillMaxWidth())
        Spacer(Modifier.height(64.dp))
    }
}