package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.usage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.ui.composables.LinearProgressBarIfEnabled
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.NbReplaysText

@Composable
internal fun UsagesTabDesktop(viewModel: UsagesViewModel) {
    Column(Modifier.fillMaxSize()) {
        LinearProgressBarIfEnabled(viewModel.isLoading)
        Column(
            Modifier.weight(1f)
                .padding(8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            NbReplaysText(viewModel.useCase, modifier = Modifier.fillMaxWidth()) // fill maxWidth to center text
            val entryBlocks = remember { viewModel.sortedPokemonMovesUsageEntries.chunked(3) }
            for (entry in entryBlocks) {
                DesktopRow(
                    viewModel,
                    entry,
                    Modifier
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
internal fun DesktopRow(
    viewModel: UsagesViewModel,
    entries: List<Map.Entry<PokemonName, PokemonUsages>>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        entries.forEach { (pokemonName, usages) ->
            Box(
                Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                PokemonUsagesCard(
                    modifier = Modifier,
                    viewModel = viewModel,
                    name = pokemonName,
                    usages = usages
                )
            }
        }
    }
}