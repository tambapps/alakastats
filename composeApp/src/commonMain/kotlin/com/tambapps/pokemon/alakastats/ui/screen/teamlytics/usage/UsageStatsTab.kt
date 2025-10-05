package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.usage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact

@Composable
fun UsageStatsTab(viewModel: UsageStatsViewModel) {
    val isCompact = LocalIsCompact.current
    LaunchedEffect(Unit) {
        viewModel.loadStats()
    }
    // TODO handle no stats case
    if (isCompact) {
        UsageStatsTabMobile(viewModel)
    } else {
        UsageStatsTabDesktop(viewModel)
    }
}


