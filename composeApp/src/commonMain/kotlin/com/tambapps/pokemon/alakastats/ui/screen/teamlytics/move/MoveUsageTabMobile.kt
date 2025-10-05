package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.move

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi


@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
internal fun MoveUsageTabMobile(viewModel: MoveUsageViewModel) {
    Column(Modifier.fillMaxWidth()
        .padding(horizontal = 4.dp)
        .verticalScroll(rememberScrollState())) {
        viewModel.pokemonMovesUsage.forEach { (pokemonName, moveUsage) ->
            PokemonMoveUsageDonut(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                viewModel = viewModel,
                name = pokemonName,
                moveUsage = moveUsage
            )
        }
    }
}