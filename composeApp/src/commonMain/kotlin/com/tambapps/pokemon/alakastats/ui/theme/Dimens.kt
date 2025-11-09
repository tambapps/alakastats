package com.tambapps.pokemon.alakastats.ui.theme

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

val LocalIsCompact = compositionLocalOf { false }

val tabReplaysTextMarginTopMobile = 32.dp
val statCardPokemonSpriteSize = 64.dp
val statCardPercentageWidth = 60.dp

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