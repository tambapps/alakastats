package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.usage

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.ui.composables.LinearProgressBarIfEnabled
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.FiltersBar
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.NbReplaysText
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.Header
import com.tambapps.pokemon.alakastats.ui.theme.teamlyticsTabPaddingBottom

@Composable
internal fun UsagesTabDesktop(viewModel: UsagesViewModel, scrollState: ScrollState) {
    Column(Modifier.fillMaxSize()) {
        LinearProgressBarIfEnabled(viewModel.isLoading)
        Column(
            Modifier.weight(1f)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            FiltersBar(viewModel)
            Spacer(Modifier.padding(16.dp))
            Header(viewModel.useCase)
            val entryBlocks = viewModel.sortedPokemonMovesUsageEntries.chunked(3)
            entryBlocks.forEachIndexed { index, entry ->
                DesktopRow(
                    viewModel,
                    entry,
                    Modifier
                )
                if (index < entryBlocks.lastIndex) {
                    Spacer(Modifier.height(32.dp))
                }
            }
            Spacer(Modifier.height(teamlyticsTabPaddingBottom))
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
                    replays = viewModel.replays,
                    pokemonImageService = viewModel.pokemonImageService,
                    name = pokemonName,
                    usages = usages
                )
            }
        }
    }
}