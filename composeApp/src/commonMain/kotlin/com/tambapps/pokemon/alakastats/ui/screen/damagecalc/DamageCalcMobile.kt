package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.ui.theme.teamlyticsTabPaddingBottom
import com.tambapps.pokemon.champions.engine.Battlefield
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val PAGES = listOf("Attacker", "Field", "Defender")
private const val ATTACKER_PAGE = 0
private const val FIELD_PAGE = 1
private const val DEFENDER_PAGE = 2
private const val RESULT_HEADER_ITEM_INDEX = 0
private const val STICKY_HEADER_ITEM_INDEX = 1
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

    // one scene for the whole screen, staying still while scrolling, so that the effects have no seam between the
    // pinned header and the rest: the screen's when it draws one behind its top bar too, else this list's
    val screenFieldEffects = LocalFieldEffectsScene.current
    val fieldEffects = screenFieldEffects ?: rememberFieldEffectsScene(viewModel.field)
    LazyColumn(
        modifier.fillMaxSize()
            .then(if (screenFieldEffects == null) Modifier.fieldEffectsScene(fieldEffects, viewModel.field) else Modifier),
        state = listState,
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
        // the moves are pinned with the compact result, to switch moves without scrolling back up
        stickyHeader {
            Column(
                Modifier.fillMaxWidth()
                    .onSizeChanged { itemHeights[STICKY_HEADER_ITEM_INDEX] = it.height }
                    .background(MaterialTheme.colorScheme.background)
                    // opaque to hide what scrolls under it, showing the screen's field effects as if it weren't
                    .fieldEffectsWindow(fieldEffects, viewModel.field)
            ) {
                AnimatedVisibility(visible = isResultHeaderScrolledAway) {
                    CompactDamageResult(viewModel)
                }
                MoveChips(viewModel, Modifier.padding(horizontal = 8.dp))
                PageTabRow(pagerState, scope, viewModel.field)
            }
        }
        item {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = pagerSwipeEnabled,
                // no background: the screen's field effects show through, still while swiping between the pages
                verticalAlignment = Alignment.Top,
            ) { page ->
                // the bottom padding is part of the pages (not of the list), so that swiping there switches pages
                val pageModifier = Modifier.fillMaxWidth()
                    .padding(8.dp)
                    .padding(bottom = teamlyticsTabPaddingBottom)
                when (page) {
                    0 -> DamageCalcPokemonPanel(
                        viewModel.attacker, DamageCalcSide.ATTACKER, team, viewModel.defender, viewModel.field,
                        onSideConditionClick = { scope.launch { pagerState.animateScrollToPage(FIELD_PAGE) } },
                        modifier = pageModifier,
                    )
                    FIELD_PAGE -> DamageCalcFieldPanel(
                        field = viewModel.field,
                        onFieldChange = { viewModel.updateField(it) },
                        onReset = { viewModel.resetField() },
                        canReset = viewModel.hasFieldState,
                        modifier = pageModifier
                    )
                    2 -> DamageCalcPokemonPanel(
                        viewModel.defender, DamageCalcSide.DEFENDER, team, viewModel.attacker, viewModel.field,
                        onSideConditionClick = { scope.launch { pagerState.animateScrollToPage(FIELD_PAGE) } },
                        modifier = pageModifier,
                    )
                }
            }
        }
    }
}

/**
 * The result of the selected move, pinned once the result header is scrolled away: the sprites of the pokemon using it
 * and of its target (as no page shows both), the damage and the KO chance
 */
@Composable
private fun CompactDamageResult(viewModel: DamageCalcViewModel) {
    Row(
        Modifier.fillMaxWidth()
            // back to the full result, like clicking the screen's title
            .clickable { viewModel.scrollToTopSignal.emit() }
            // like the result card, without its border: a line across the whole screen, on top of the pinned header
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalcPokemonSprites(viewModel)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            when (val result = viewModel.result) {
                is DamageCalcResult.Success -> {
                    Text(
                        result.shownDamageText,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        result.koChanceText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is DamageCalcResult.Error -> Text(
                    result.message,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun PageTabRow(pagerState: PagerState, scope: CoroutineScope, field: Battlefield) {
    SecondaryTabRow(
        selectedTabIndex = pagerState.currentPage,
        // the field effects drawn behind the header showing through. No line in their colors under the tabs: going
        // from the weather's under Attacker to the terrain's under Defender, it would look like each applies to one side
        containerColor = Color.Transparent,
    ) {
        PAGES.forEachIndexed { index, title ->
            Tab(
                selected = pagerState.currentPage == index,
                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                text = {
                    // the active weather and terrain, seen from any page
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (index == FIELD_PAGE) {
                            WeatherIcon(field.weather, Modifier.size(TAB_ICON_SIZE))
                            TerrainIcon(field.terrain, Modifier.size(TAB_ICON_SIZE))
                        }
                        // the conditions on for each side, one dot per category, on the side of the field: after the
                        // attacker's name, before the defender's
                        if (index == DEFENDER_PAGE) SideConditionDots(field.defenderSide)
                        Text(title, maxLines = 1, softWrap = false)
                        if (index == ATTACKER_PAGE) SideConditionDots(field.attackerSide)
                    }
                }
            )
        }
    }
}

private val TAB_ICON_SIZE = 18.dp
