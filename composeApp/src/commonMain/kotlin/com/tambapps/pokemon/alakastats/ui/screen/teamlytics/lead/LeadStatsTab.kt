package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.lead

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact

@Composable
fun LeadStatsTab(viewModel: LeadStatsViewModel) {
    val isCompact = LocalIsCompact.current
    LaunchedEffect(Unit) {
        viewModel.loadStats()
    }
    if (isCompact) {
        LeadStatsTabMobile(viewModel)
    } else {
        LeadStatsTabDesktop(viewModel)
    }
}