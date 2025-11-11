package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.lead

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.composables.LinearProgressBarIfEnabled
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.NbReplaysText
import com.tambapps.pokemon.alakastats.ui.theme.teamlyticsTabPaddingBottom

@Composable
internal fun LeadStatsTabDesktop(viewModel: LeadStatsViewModel) {
    Column(Modifier.fillMaxSize()) {
        LinearProgressBarIfEnabled(viewModel.isLoading)
        Column(Modifier.fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp, bottom = 8.dp)) {
            NbReplaysText(viewModel.useCase, modifier = Modifier.fillMaxWidth()) // fill maxWidth to center text
            Spacer(Modifier.height(64.dp))
            LeadAndWinRow(viewModel)
            Space()
            MostEffectiveLeadRow(viewModel)
            Space()
            MostCommonLeadRow(viewModel)
            Spacer(Modifier.height(teamlyticsTabPaddingBottom))
        }
    }
}

@Composable
private fun Space() = Spacer(Modifier.height(64.dp))