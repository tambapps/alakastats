package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.usage

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.composables.LinearProgressBarIfEnabled
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.FiltersBar
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.NbReplaysText
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.Header
import com.tambapps.pokemon.alakastats.ui.theme.tabReplaysTextMarginTopMobile
import com.tambapps.pokemon.alakastats.ui.theme.teamlyticsTabPaddingBottom
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi


@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
internal fun UsagesTabMobile(viewModel: UsagesViewModel, scrollState: ScrollState) {
    Column(
        Modifier.fillMaxWidth()
    ) {
        Column(Modifier.weight(1f)
            .padding(horizontal = 4.dp)
            .verticalScroll(scrollState)) {
            Spacer(Modifier.height(tabReplaysTextMarginTopMobile))
            FiltersBar(viewModel)
            Spacer(Modifier.height(16.dp))
            Header(viewModel.useCase)

            val entries = viewModel.sortedPokemonMovesUsageEntries
            entries.forEach { (pokemonName, moveUsage) ->
                PokemonUsagesCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    replays = viewModel.replays,
                    pokemonImageService = viewModel.pokemonImageService,
                    name = pokemonName,
                    usages = moveUsage
                )
            }
            Spacer(Modifier.height(teamlyticsTabPaddingBottom))
        }
        LinearProgressBarIfEnabled(viewModel.isLoading)
    }
}