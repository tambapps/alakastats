package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tambapps.pokemon.alakastats.ui.composables.TabRowWithBackButton

@Composable
internal fun ColumnScope.TeamlyticsScreenMobile(
    viewModel: TeamlyticsViewModel,
    tabs: List<String>,
    pagerState: PagerState,
) {
    Pager(Modifier.weight(1f), viewModel, pagerState)
    TabRowWithBackButton(viewModel, pagerState, tabs, Modifier.weight(1f))
}