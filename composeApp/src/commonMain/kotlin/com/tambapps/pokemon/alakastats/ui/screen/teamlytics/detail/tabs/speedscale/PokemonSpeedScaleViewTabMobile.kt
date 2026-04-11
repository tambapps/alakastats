package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail.tabs.speedscale

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.composables.ExpansionTile


@Composable
fun PokemonSpeedScaleViewTabMobile(
    viewModel: PokemonSpeedScaleViewModel,
    scrollState: LazyListState
) {
    Column(Modifier.fillMaxSize()) {
        SettingsBar(viewModel)
        SpeedScale(viewModel, scrollState, Modifier.weight(1f))
    }
}

@Composable
private fun SettingsBar(viewModel: PokemonSpeedScaleViewModel) {
    ExpansionTile(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        title = {
            Text("Speed Settings", style = MaterialTheme.typography.titleLarge)
        },
        subtitle = { isExpanded ->
            if (isExpanded) {
                return@ExpansionTile
            }
            val opposingString = buildString {
                if (viewModel.stage != 0) append(if (viewModel.stage > 0) "+${viewModel.stage}" else viewModel.stage).append(" ")
                when {
                    viewModel.maxEvs && viewModel.speedNature -> append("${viewModel.maxEvsValue}+ ")
                    viewModel.maxEvs -> append("${viewModel.maxEvsValue} ")
                    viewModel.speedNature -> append("+Spe Nature ")
                }
                if (viewModel.scarfBoost) append("Scarf")
            }
            opposingString.takeIf { !it.isBlank() }?.let { Text("Opposing Investments: $it") }
            val pokemonString = buildString {
                if (viewModel.ownStage != 0) append(if (viewModel.ownStage > 0) "+${viewModel.ownStage}" else viewModel.ownStage).append(" ")
                if (viewModel.ownScarfBoost) append("Scarf")
            }
            pokemonString.takeIf { !it.isBlank() }?.let { Text("${viewModel.pokemon.name.pretty}: $it") }
        }
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text("Opposing Investments", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            OpposingInvestmentsFlowRow(viewModel)

            Spacer(Modifier.height(16.dp))
            Text("${viewModel.pokemon.name.pretty}'s Boosts", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            PokemonBoostsFlowRow(viewModel)
        }

    }

}

