package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// allows to define scrollbars for wasm
@Composable
expect fun ScrollableRow(
    modifier: Modifier,
    scrollState: ScrollState,
    scrollbarThickness: Dp = 8.dp,
    content: @Composable (RowScope.() -> Unit)
)