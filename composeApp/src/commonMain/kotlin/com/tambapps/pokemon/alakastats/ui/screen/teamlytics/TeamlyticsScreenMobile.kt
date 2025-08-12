package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton

@Composable
fun ColumnScope.TeamlyticsScreenMobile(
    viewModel: TeamlyticsViewModel,
    tabs: List<String>,
    pagerState: PagerState
) {
    Pager(viewModel, pagerState)
    Row(Modifier.fillMaxWidth()) {
        val navigator = LocalNavigator.currentOrThrow
        BackIconButton(navigator)
        ScrollableTabRow(
            modifier = Modifier.weight(1f),
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 16.dp
        ) {
            TabRowContent(pagerState, tabs)
        }

    }
}