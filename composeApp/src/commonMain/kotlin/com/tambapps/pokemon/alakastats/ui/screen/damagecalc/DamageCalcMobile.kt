package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.ui.theme.teamlyticsTabPaddingBottom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val PAGES = listOf("Attacker", "Field", "Defender")
private const val FIELD_PAGE = 1
private const val RESULT_HEADER_ITEM_INDEX = 0
private const val MOVES_ITEM_INDEX = 1
private const val STICKY_HEADER_ITEM_INDEX = 2
private const val SCROLL_TO_TOP_DURATION_MILLIS = 300

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DamageCalcMobile(
    viewModel: DamageCalcViewModel,
    modifier: Modifier = Modifier,
    team: Teamlytics? = null,
    pagerSwipeEnabled: Boolean = true,
) {
    val pagerState = rememberPagerState(pageCount = { PAGES.size })
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val isResultHeaderScrolledAway by remember {
        derivedStateOf { listState.firstVisibleItemIndex > RESULT_HEADER_ITEM_INDEX }
    }
    // heights of the items above the pager, needed to know the distance to scroll back to the top
    val itemHeights = remember { mutableStateMapOf<Int, Int>() }
    viewModel.scrollToTopSignal.Listen {
        val distance = (0 until listState.firstVisibleItemIndex).sumOf { itemHeights[it] ?: 0 } +
                listState.firstVisibleItemScrollOffset
        if (distance > 0) {
            listState.animateScrollBy(-distance.toFloat(), tween(SCROLL_TO_TOP_DURATION_MILLIS))
            // the sticky header shrinks while scrolling up, make sure we end up at the very top
            listState.scrollToItem(0)
        }
    }

    LazyColumn(
        modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = teamlyticsTabPaddingBottom)
    ) {
        item {
            DamageResultHeader(
                viewModel = viewModel,
                onFieldSummaryClick = { scope.launch { pagerState.animateScrollToPage(FIELD_PAGE) } },
                modifier = Modifier
                    .onSizeChanged { itemHeights[RESULT_HEADER_ITEM_INDEX] = it.height }
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }
        item {
            MoveChips(
                viewModel,
                Modifier.onSizeChanged { itemHeights[MOVES_ITEM_INDEX] = it.height }
                    .padding(horizontal = 8.dp)
            )
        }
        stickyHeader {
            Column(
                Modifier.fillMaxWidth()
                    .onSizeChanged { itemHeights[STICKY_HEADER_ITEM_INDEX] = it.height }
                    .background(MaterialTheme.colorScheme.background)
            ) {
                AnimatedVisibility(visible = isResultHeaderScrolledAway) {
                    CompactDamageResult(viewModel)
                }
                PageTabRow(pagerState, scope)
            }
        }
        item {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = pagerSwipeEnabled,
                verticalAlignment = Alignment.Top,
            ) { page ->
                val pageModifier = Modifier.fillMaxWidth().padding(8.dp)
                when (page) {
                    0 -> DamageCalcPokemonPanel(viewModel.attacker, DamageCalcSide.ATTACKER, team, viewModel.defender, pageModifier)
                    FIELD_PAGE -> DamageCalcFieldPanel(
                        field = viewModel.field,
                        onFieldChange = { viewModel.field = it },
                        modifier = pageModifier
                    )
                    2 -> DamageCalcPokemonPanel(viewModel.defender, DamageCalcSide.DEFENDER, team, viewModel.attacker, pageModifier)
                }
            }
        }
    }
}

@Composable
private fun CompactDamageResult(viewModel: DamageCalcViewModel) {
    val text = when (val result = viewModel.result) {
        is DamageCalcResult.Success -> "${result.damagePercentText} · ${result.koChanceText}"
        is DamageCalcResult.Error -> result.message
    }
    Text(
        text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun PageTabRow(pagerState: PagerState, scope: CoroutineScope) {
    SecondaryTabRow(selectedTabIndex = pagerState.currentPage) {
        PAGES.forEachIndexed { index, title ->
            Tab(
                selected = pagerState.currentPage == index,
                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                text = { Text(title) }
            )
        }
    }
}
