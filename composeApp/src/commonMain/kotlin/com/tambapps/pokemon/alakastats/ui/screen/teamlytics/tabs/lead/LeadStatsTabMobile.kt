package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.lead

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.tambapps.pokemon.alakastats.ui.theme.tabReplaysTextMarginTopMobile
import com.tambapps.pokemon.alakastats.ui.theme.teamlyticsTabPaddingBottom

@Composable
internal fun LeadStatsTabMobile(viewModel: LeadStatsViewModel, scrollState: ScrollState) {
    Column(
        Modifier.fillMaxSize()
    ) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 8.dp)
                .weight(1f)
        ) {
            if (viewModel.isLoading) {
                return@Column
            }
            Spacer(Modifier.height(tabReplaysTextMarginTopMobile))
            FiltersBar(viewModel)
            Spacer(Modifier.height(16.dp))
            NbReplaysText(viewModel.useCase, modifier = Modifier.fillMaxWidth()) // fill maxWidth to center text
            Spacer(Modifier.height(64.dp))
            LeadAndWinRow(viewModel)
            Space()
            MostEffectiveLeadRow(viewModel)
            Space()
            MostCommonLeadRow(viewModel)
            Spacer(Modifier.height(teamlyticsTabPaddingBottom))
        }
        LinearProgressBarIfEnabled(viewModel.isLoading)
    }
}

@Composable
private fun Space() = Spacer(Modifier.height(42.dp))

