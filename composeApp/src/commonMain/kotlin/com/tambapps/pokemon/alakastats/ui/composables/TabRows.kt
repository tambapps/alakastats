package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.util.VoidSignal
import kotlinx.coroutines.launch

interface PagerViewModel {

    var scrollToTopIndex: Int?
}

interface TabViewModel {

    val scrollToTopSignal: VoidSignal

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


@Composable
fun EmitScrollEffect(pagerViewModel: PagerViewModel, tabViewModel: TabViewModel, page: Int) {
    LaunchedEffect(pagerViewModel.scrollToTopIndex) {
        if (page == pagerViewModel.scrollToTopIndex) {
            tabViewModel.scrollToTopSignal.emit()
            pagerViewModel.scrollToTopIndex = null
        }
    }
}

@Composable
fun ScrollToTopIfNeeded(viewModel: TabViewModel, scrollState: ScrollState) {
    viewModel.scrollToTopSignal.Listen {
        if (scrollState.value != 0) {
            scrollState.animateScrollTo(
                value = 0,
                animationSpec = tween(durationMillis = 500)
            )
        }
    }
}

@Composable
fun ScrollToTopIfNeeded(viewModel: TabViewModel, scrollState: LazyListState) {
    viewModel.scrollToTopSignal.Listen {
        if (scrollState.firstVisibleItemIndex != 0) {
            scrollState.animateScrollToItem(index = 0)
        }
    }
}