package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail.tabs.overview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.composables.PokemonMoves
import com.tambapps.pokemon.alakastats.ui.composables.PokemonStatsRow
import com.tambapps.pokemon.alakastats.ui.composables.PokepastePokemonItemAndAbility
import com.tambapps.pokemon.alakastats.ui.composables.ScrollToTopIfNeeded
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact


@Composable
fun PokemonDetailsOverviewTab(
    viewModel: PokemonDetailOverviewModel,
) {
    val scrollState = rememberScrollState()
    if (LocalIsCompact.current) {
        PokemonDetailsOverviewMobile(viewModel, scrollState)
    } else {
        PokemonDetailsOverviewDesktop(viewModel, scrollState)
    }
    ScrollToTopIfNeeded(viewModel, scrollState)
}

@Composable
internal fun PokemonDetailsOverview(
    viewModel: PokemonDetailOverviewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        PokepastePokemonItemAndAbility(viewModel.pokemon, viewModel.pokemonImageService)
        Spacer(Modifier.height(16.dp))
        if (!viewModel.notes.isNullOrBlank()) {
            Text(viewModel.notes, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
        }
        if (!viewModel.team.pokePaste.isOts) {
            if (viewModel.pokemonData != null) {
                Text("Stats (lvl ${viewModel.pokemon.level})", style = MaterialTheme.typography.headlineSmall)
                PokemonStatsRow(viewModel.pokemon, viewModel.pokemonData, Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
            }
            Text("Investments", style = MaterialTheme.typography.headlineSmall)
            PokemonStatsRow(viewModel.pokemon, pokemonData=null, Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }
        Text("Moves", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        PokemonMoves(viewModel.pokemon, viewModel.pokemonImageService)
    }
}