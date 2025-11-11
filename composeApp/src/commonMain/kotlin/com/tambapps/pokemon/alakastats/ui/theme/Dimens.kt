package com.tambapps.pokemon.alakastats.ui.theme

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

val LocalIsCompact = compositionLocalOf { false }

// allow to scroll past the Fab button(s)
val teamlyticsTabPaddingBottom = 128.dp

val fabPadding @Composable get() = if (LocalIsCompact.current) 32.dp else 50.dp

val tabReplaysTextMarginTopMobile = 32.dp

@Composable
fun ProvideIsCompact(
    content: @Composable () -> Unit
) {
    BoxWithConstraints {
        val isCompact = maxWidth < 600.dp
        CompositionLocalProvider(LocalIsCompact provides isCompact) {
            content()
        }
    }
}