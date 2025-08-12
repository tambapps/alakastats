package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable

@Composable
fun ColumnScope.TeamlyticsScreenDesktop(
    viewModel: TeamlyticsViewModel,
    tabs: List<String>,
    pagerState: PagerState
) {
    TabRow(
        selectedTabIndex = pagerState.currentPage
    ) {
        TabRowContent(pagerState, tabs)
    }
    Pager(viewModel, pagerState)
}