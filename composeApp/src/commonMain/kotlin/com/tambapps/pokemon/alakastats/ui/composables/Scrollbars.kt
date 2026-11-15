package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val defaultScrollbarThickness = 8.dp
// allows to define scrollbars for wasm
@Composable
expect fun ScrollableRow(
    modifier: Modifier,
    scrollState: ScrollState,
    scrollbarThickness: Dp = defaultScrollbarThickness,
    content: @Composable RowScope.() -> Unit
)

/**
 * A lazy column with, on the desktop web, a scrollbar on its right
 */
@Composable
expect fun LazyColumnWithScrollbar(
    modifier: Modifier,
    state: LazyListState = rememberLazyListState(),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    scrollbarThickness: Dp = defaultScrollbarThickness,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyListScope.() -> Unit
)
/**
 * A lazy vertical grid with, on the desktop web, a scrollbar on its right
 */
@Composable
expect fun LazyVerticalGridWithScrollbar(
    columns: GridCells,
    modifier: Modifier,
    state: LazyGridState = rememberLazyGridState(),
    scrollbarThickness: Dp = defaultScrollbarThickness,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyGridScope.() -> Unit
)
