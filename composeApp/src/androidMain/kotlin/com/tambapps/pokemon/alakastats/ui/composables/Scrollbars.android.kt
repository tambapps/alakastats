package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
    verticalArrangement: Arrangement.Vertical,
    contentPadding: PaddingValues,
    content: LazyListScope.() -> Unit
) = LazyColumn(
    modifier = modifier,
    horizontalAlignment = horizontalAlignment,
    verticalArrangement = verticalArrangement,
    contentPadding = contentPadding,
    state = state,
    content = content,
)
@Composable
actual fun LazyVerticalGridWithScrollbar(
    columns: GridCells,
    modifier: Modifier,
    state: LazyGridState,
    scrollbarThickness: Dp,
    verticalArrangement: Arrangement.Vertical,
    horizontalArrangement: Arrangement.Horizontal,
    contentPadding: PaddingValues,
    content: LazyGridScope.() -> Unit
) = LazyVerticalGrid(
    columns = columns,
    modifier = modifier,
    state = state,
    verticalArrangement = verticalArrangement,
    horizontalArrangement = horizontalArrangement,
    contentPadding = contentPadding,
    content = content,
)
