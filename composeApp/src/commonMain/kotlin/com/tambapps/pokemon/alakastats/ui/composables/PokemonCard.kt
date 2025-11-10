package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


@Composable
fun PokemonCard(
    modifier: Modifier = Modifier,
    pokemonArtwork: @Composable BoxScope.(Dp, Dp) -> Unit,
    cardContent: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        var dimensions by remember { mutableStateOf(0.dp to 0.dp) }
        val (contentWidth, contentHeight) = dimensions
        MyCard(modifier = Modifier.fillMaxWidth(0.9f)
            // + for padding
            .height(remember(contentHeight) { contentHeight + 16.dp }), gradientBackgroundColors = elevatedCardGradientColors) {}

        pokemonArtwork.invoke(this, contentWidth, contentHeight)

        val density = LocalDensity.current
        Box(
            modifier = Modifier.fillMaxWidth(0.825f)
                .fillMaxHeight(0.85f)
                .onSizeChanged { size ->
                    with(density) { dimensions = size.width.toDp() to size.height.toDp() }
                }
        ) {
            cardContent.invoke(this)
        }
    }
}