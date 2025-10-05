package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.notes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics


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
        Spacer(Modifier.width(16.dp))
        Text("This team is about doing this and that")
        Spacer(Modifier.width(16.dp))
        Spacer(Modifier.width(16.dp))
        for (pokemon in team.pokePaste.pokemons) {
            PokemonMobile(viewModel, team, pokemon)
        }
    }
}

@Composable
private fun PokemonMobile(viewModel: TeamNotesViewModel, team: Teamlytics, pokemon: Pokemon) {
    Text(pokemon.name.pretty, style = MaterialTheme.typography.headlineMedium)
    viewModel.pokemonImageService.PokemonArtwork(pokemon.name)
    Text("This Pokemon does this and that")
}