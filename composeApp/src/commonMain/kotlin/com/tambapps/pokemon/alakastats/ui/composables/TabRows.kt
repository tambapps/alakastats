package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import kotlinx.coroutines.launch

interface PagerViewModel {

    var scrollToTopIndex: Int?
}

@Composable
fun TabRowWithBackButton(
    viewModel: PagerViewModel,
    pagerState: PagerState,
    tabs: List<String>,
    modifier: Modifier = Modifier
) {
    Row(Modifier.fillMaxWidth().background(TabRowDefaults.secondaryContainerColor)) {
        val navigator = LocalNavigator.currentOrThrow
        BackIconButton(navigator)
        if (LocalIsCompact.current) {
            SecondaryScrollableTabRow(
                modifier = modifier,
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 16.dp
            ) {
                TabRowContent(viewModel, pagerState, tabs)
            }
        } else {
            SecondaryTabRow(
                modifier = modifier,
                selectedTabIndex = pagerState.currentPage,
            ) {
                TabRowContent(viewModel, pagerState, tabs)
            }
        }
    }
}

@Composable
private fun TabRowContent(
    viewModel: PagerViewModel,
    pagerState: PagerState,
    tabs: List<String>
) {
    val scope = rememberCoroutineScope()
    tabs.forEachIndexed { index, title ->
        Tab(
            selected = pagerState.currentPage == index,
            onClick = {
                if (pagerState.currentPage == index) {
                    viewModel.scrollToTopIndex = index
                } else {
                    scope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            },
            text = { Text(title) }
        )
    }
}