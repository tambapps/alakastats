package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun MyCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = onClick != null,
    border: BorderStroke? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: CardColors = CardDefaults.elevatedCardColors(),
    gradientBackground: Boolean = false,
    elevation: CardElevation = CardDefaults.elevatedCardElevation(8.dp),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var cardModifier = modifier
    if (border != null) {
        cardModifier = cardModifier.border(border, shape)
    }

    val cardColors = if (gradientBackground) colors.copy(containerColor = Color.Transparent) else colors
    ElevatedCard(
        modifier = cardModifier,
        onClick = onClick ?: {},
        enabled = enabled,
        shape = shape,
        colors = cardColors,
        elevation = elevation,
        interactionSource = interactionSource,
    ) {
        if (gradientBackground) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .gradientCardBackground()
            ) {
                content()
            }
        } else {
            content()
        }
    }
}

@Composable
fun Modifier.gradientCardBackground() =
    background(
        brush = Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                MaterialTheme.colorScheme.surfaceContainerLow
            )
        )
    )
