package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.lead

import androidx.compose.runtime.Composable
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact

@Composable
fun LeadStatsTab(viewModel: LeadStatsViewModel) {
    val isCompact = LocalIsCompact.current
    if (isCompact) {
        LeadStatsTabMobile(viewModel)
    } else {
        LeadStatsTabDesktop(viewModel)
    }
}