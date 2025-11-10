package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// allows to define scrollbars for wasm
@Composable
expect fun ScrollableRow(
    modifier: Modifier,
    scrollState: ScrollState,
    content: @Composable (RowScope.() -> Unit)
)