package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.move

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.ui.composables.LinearProgressBarIfEnabled

@Composable
internal fun MoveUsageTabDesktop(viewModel: MoveUsageViewModel) {
    Column(Modifier.fillMaxSize()) {
        LinearProgressBarIfEnabled(viewModel.isLoading)
        Column(
            Modifier.weight(1f)
                .padding(8.dp)
        ) {
            val entryBlocks = remember { viewModel.sortedPokemonMovesUsageEntries.chunked(3) }

            for (entry in entryBlocks) {
                DesktopRow(
                    viewModel,
                    entry,
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
internal fun DesktopRow(
    viewModel: MoveUsageViewModel,
    entries: List<Map.Entry<PokemonName, Map<String, Int>>>,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth()) {
        entries.forEach { (pokemonName, moveUsage) ->
            Box(
                Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                PokemonMoveUsageDonut(
                    modifier = Modifier,
                    viewModel = viewModel,
                    name = pokemonName,
                    moveUsage = moveUsage
                )
            }
        }
    }
}