package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.matchup

import androidx.compose.runtime.Composable
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact


@Composable
fun MatchupsTab(viewModel: MatchupsViewModel) {
    if (LocalIsCompact.current) {
        MatchupsTabMobile(viewModel)
    } else {
        MatchupsTabDesktop(viewModel)
    }
}