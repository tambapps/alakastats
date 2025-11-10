package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun ScrollableRow(
    modifier: Modifier,
    scrollState: ScrollState,
    content: @Composable RowScope.() -> Unit
) {
    Row(modifier.horizontalScroll(scrollState), content = content)
}