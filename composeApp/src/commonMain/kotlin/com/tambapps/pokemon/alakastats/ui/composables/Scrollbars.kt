package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyListScope
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

@Composable
expect fun LazyColumnWithScrollbar(
    modifier: Modifier,
    state: LazyListState = rememberLazyListState(),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    scrollbarThickness: Dp = defaultScrollbarThickness,
    content: LazyListScope.() -> Unit
)