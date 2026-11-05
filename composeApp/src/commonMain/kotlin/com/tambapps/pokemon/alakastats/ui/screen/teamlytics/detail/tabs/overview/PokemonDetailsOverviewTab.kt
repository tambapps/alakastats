package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail.tabs.overview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.ui.composables.PokemonMoves
import com.tambapps.pokemon.alakastats.ui.composables.PokemonStatsRadar
import com.tambapps.pokemon.alakastats.ui.composables.PokepastePokemonItemAndAbility
import com.tambapps.pokemon.alakastats.ui.composables.ScrollToTopIfNeeded
import com.tambapps.pokemon.alakastats.ui.composables.StaticPokemonStatsRow
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
    pokemon: Pokemon,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        PokepastePokemonItemAndAbility(pokemon)
        Spacer(Modifier.height(16.dp))
        if (!viewModel.notes.isNullOrBlank()) {
            Text(
                viewModel.notes,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        val baseStats = viewModel.pokemonData?.baseStatsOf(pokemon.name)
        when {
            viewModel.team.pokePaste.isOts -> MovesSection(pokemon)
            baseStats == null -> {
                Text("Investments", style = MaterialTheme.typography.headlineSmall)
                StaticPokemonStatsRow(pokemon, pokemonData = null, viewModel.team.format, Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                MovesSection(pokemon)
            }
            LocalIsCompact.current -> {
                StatsSection(pokemon, baseStats, viewModel.team.format, Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                MovesSection(pokemon)
            }
            else -> Row(Modifier.fillMaxWidth()) {
                MovesSection(pokemon, Modifier.padding(top = 72.dp))
                Spacer(Modifier.width(16.dp))
                StatsSection(pokemon, baseStats, viewModel.team.format, Modifier.weight(1f), Alignment.CenterHorizontally)
            }
        }
    }
}

@Composable
private fun StatsSection(
    pokemon: Pokemon,
    baseStats: PokeStats,
    format: Format,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(modifier, horizontalAlignment = horizontalAlignment) {
        Text("Stats (lvl ${pokemon.level})", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        PokemonStatsRadar(pokemon, baseStats, format, Modifier.fillMaxWidth())
    }
}

@Composable
private fun MovesSection(pokemon: Pokemon, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text("Moves", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        PokemonMoves(pokemon)
    }
}