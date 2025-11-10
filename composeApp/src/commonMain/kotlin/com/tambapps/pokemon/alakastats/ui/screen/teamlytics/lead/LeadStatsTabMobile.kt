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
import com.tambapps.pokemon.alakastats.ui.theme.tabReplaysTextMarginTopMobile
import com.tambapps.pokemon.alakastats.ui.theme.teamlyticsPaddingBottomMobile

@Composable
internal fun LeadStatsTabMobile(viewModel: LeadStatsViewModel) {
    Column(
        Modifier.fillMaxSize()
    ) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp)
                .weight(1f)
        ) {
            if (viewModel.isLoading) {
                return@Column
            }
            Spacer(Modifier.height(tabReplaysTextMarginTopMobile))
            NbReplaysText(viewModel.useCase, modifier = Modifier.fillMaxWidth()) // fill maxWidth to center text
            Spacer(Modifier.height(64.dp))
            LeadAndWinRow(viewModel)
            MostCommonLeadCard(viewModel)
            Space()
            MostEffectiveLeadCard(viewModel)
            Space()
            LeadAndWin(viewModel)
            Spacer(Modifier.height(teamlyticsPaddingBottomMobile))
        }
        LinearProgressBarIfEnabled(viewModel.isLoading)
    }
}

@Composable
private fun Space() = Spacer(Modifier.height(42.dp))

