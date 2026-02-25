package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.ui.service.FacingDirection
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import kotlin.math.pow

@Composable
fun PokemonStatCard(
    pokemonImageService: PokemonImageService,
    title: String,
    text: String,
    pokemonName: PokemonName,
    pokemonName2: PokemonName? = null,
    modifier: Modifier) {
    val density = LocalDensity.current
    val (minOffsetDp, maxOffsetDp) = if (pokemonName2 == null) 12.dp  to 80.dp else 24.dp to 100.dp
    PokemonCard(
        modifier = modifier,
        pokemonArtwork = { contentWidth, contentHeight ->
            var spriteWidth by remember { mutableStateOf(0.dp) }
            pokemonImageService.PokemonArtwork(
                name = pokemonName,
                modifier = Modifier.align(Alignment.BottomEnd)
                    .height(if (LocalIsCompact.current) 175.dp else 200.dp)
                    // to avoid artworks like basculegion's to take the whole width and make the moves difficult to read
                    .widthIn(max = remember(contentWidth) { contentWidth * 0.7f })
                    .onSizeChanged { size ->
                        with(density) { spriteWidth = size.width.toDp() }
                    }
                    .offset(y = 16.dp, x = leadOffsetDp(
                        spriteWidth,
                        minOffsetDp = minOffsetDp,
                        maxOffsetDp = maxOffsetDp
                    ))
            )
            pokemonName2?.let {
                var spriteWidth by remember { mutableStateOf(0.dp) }

                pokemonImageService.PokemonArtwork(
                    name = it,
                    facingDirection = FacingDirection.RIGHT,
                    modifier = Modifier.align(Alignment.BottomStart)
                        .height(if (LocalIsCompact.current) 175.dp else 200.dp)
                        // to avoid artworks like basculegion's to take the whole width and make the moves difficult to read
                        .widthIn(max = remember(contentWidth) { contentWidth * 0.7f })
                        .onSizeChanged { size ->
                            with(density) { spriteWidth = size.width.toDp() }
                        }
                        .offset(y = 16.dp, x = - leadOffsetDp(
                            spriteWidth,
                            minOffsetDp = minOffsetDp,
                            maxOffsetDp = maxOffsetDp
                        ))
                )
            }
        }
    ) {
        Text(
            title,
            modifier = Modifier.align(Alignment.TopCenter),
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center,
        )

        Text(
            text,
            textAlign = if (pokemonName2 != null) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.align(if (pokemonName2 != null) Alignment.BottomCenter else Alignment.BottomStart).padding(bottom = 8.dp),
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

private fun leadOffsetDp(
    widthDp: Dp,
    minOffsetDp: Dp,     // thin sprites
    maxOffsetDp: Dp,     // very wide sprites
    wMinDp: Dp = 56.dp,          // tune to your art
    wMaxDp: Dp = 160.dp,         // tune to your art
    gamma: Double = 1.8
): Dp {
    val w = widthDp.value
    val wMin = wMinDp.value
    val wMax = wMaxDp.value
    val t = ((w - wMin) / (wMax - wMin)).coerceIn(0f, 1f)
    val eased = t.toDouble().pow(gamma).toFloat()
    val off = minOffsetDp.value + (maxOffsetDp.value - minOffsetDp.value) * eased
    return off.dp
}



@Composable
fun PokemonCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    pokemonArtwork: @Composable BoxScope.(Dp, Dp) -> Unit,
    cardContent: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        var dimensions by remember { mutableStateOf(0.dp to 0.dp) }
        val (contentWidth, contentHeight) = dimensions
        val interactionSource = remember { MutableInteractionSource() }
        MyCard(
            modifier = Modifier.fillMaxWidth(0.9f)
                // + for padding
                .height(remember(contentHeight) { contentHeight + 16.dp }),
            gradientBackgroundColors = elevatedCardGradientColors,
            onClick = onClick,
            interactionSource = interactionSource
        ) {}

        pokemonArtwork.invoke(this, contentWidth, contentHeight)

        val density = LocalDensity.current
        Box(
            modifier = Modifier.fillMaxWidth(0.825f)
                .fillMaxHeight(0.85f)
                .onSizeChanged { size ->
                    with(density) { dimensions = size.width.toDp() to size.height.toDp() }
                }
                .then(if (onClick != null) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ) else Modifier)
        ) {
            cardContent.invoke(this)
        }
    }
}
