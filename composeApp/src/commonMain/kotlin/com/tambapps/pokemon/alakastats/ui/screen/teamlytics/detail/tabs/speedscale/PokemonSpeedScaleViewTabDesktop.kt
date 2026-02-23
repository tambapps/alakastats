package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail.tabs.speedscale

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun PokemonSpeedScaleViewTabDesktop(
    viewModel: PokemonSpeedScaleViewModel,
    scrollState: LazyListState
) {
    Column(Modifier.fillMaxSize()) {
        SettingsBar(viewModel)
        SpeedScale(viewModel, scrollState, Modifier.weight(1f))
    }

}

@Composable
private fun SettingsBar(viewModel: PokemonSpeedScaleViewModel, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Opposing Investments", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                OpposingInvestmentsFlowRow(viewModel)
            }
            Spacer(Modifier.width(16.dp))
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text("${viewModel.pokemon.name.value}'s Boosts", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                PokemonBoostsFlowRow(viewModel)
            }
        }

    }

}
