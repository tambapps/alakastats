package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact

@Composable
actual fun ScrollableRow(
    modifier: Modifier,
    scrollState: ScrollState,
    content: @Composable RowScope.() -> Unit
) {
    if (LocalIsCompact.current) {
        Row(modifier.horizontalScroll(scrollState), content = content)
    } else {
        // defined scrollbar for desktop Web
        Box(modifier) {
            Row(modifier = Modifier.horizontalScroll(scrollState).fillMaxSize(), content = content)

            HorizontalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxWidth(),
            )
        }
    }
}