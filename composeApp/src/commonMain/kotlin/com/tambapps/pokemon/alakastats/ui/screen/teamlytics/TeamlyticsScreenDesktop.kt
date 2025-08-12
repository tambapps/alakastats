package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton

@Composable
fun ColumnScope.TeamlyticsScreenDesktop(
    viewModel: TeamlyticsViewModel,
    tabs: List<String>,
    pagerState: PagerState
) {

    Row(Modifier.fillMaxWidth()) {
        val navigator = LocalNavigator.currentOrThrow
        BackIconButton(navigator)
        TabRow(
            selectedTabIndex = pagerState.currentPage
        ) {
            TabRowContent(pagerState, tabs)
        }
    }
    Pager(viewModel, pagerState)
}