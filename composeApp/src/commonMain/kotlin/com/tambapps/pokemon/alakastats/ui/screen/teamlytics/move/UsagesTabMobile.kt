package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.move

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.composables.LinearProgressBarIfEnabled
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi


@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
internal fun UsagesTabMobile(viewModel: UsagesViewModel) {
    Column(
        Modifier.fillMaxWidth()
    ) {
        Column(Modifier.weight(1f)
            .padding(horizontal = 4.dp)
            .verticalScroll(rememberScrollState())) {
            ReplayCountText(viewModel)

            val entries = viewModel.sortedPokemonMovesUsageEntries
            entries.forEach { (pokemonName, moveUsage) ->
                PokemonUsagesCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    viewModel = viewModel,
                    name = pokemonName,
                    usages = moveUsage
                )
            }
            Spacer(Modifier.height(64.dp))
        }
        LinearProgressBarIfEnabled(viewModel.isLoading)
    }
}