package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
actual fun ScrollableRow(
    modifier: Modifier,
    scrollState: ScrollState,
    scrollbarThickness: Dp,
    content: @Composable RowScope.() -> Unit
) = Row(modifier.horizontalScroll(scrollState), content = content)

@Composable
actual fun LazyColumnWithScrollbar(
    modifier: Modifier,
    state: LazyListState,
    horizontalAlignment: Alignment.Horizontal,
    scrollbarThickness: Dp,
    content: LazyListScope.() -> Unit
) = LazyColumn(modifier = modifier, horizontalAlignment = horizontalAlignment, state = state, content = content)