package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.matchup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact


@Composable
fun MatchupsTab(viewModel: MatchupsViewModel) {
    LaunchedEffect(viewModel.useCase.filters) {
        viewModel.loadStats()
    }
    if (LocalIsCompact.current) {
        MatchupsTabMobile(viewModel)
    } else {
        MatchupsTabDesktop(viewModel)
    }
}