package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.lead

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun LeadStatsTabMobile(viewModel: LeadStatsViewModel) {
    Column(
        Modifier.verticalScroll(rememberScrollState())
    ) {
        MostCommonLeadCard(viewModel)
        Spacer(Modifier.height(32.dp))
        MostEffectiveLeadCard(viewModel)
    }
}
