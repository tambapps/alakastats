package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.alakastats.ui.theme.color

// above it, a type's color is too light for a white text (e.g. Electric, Ice)
private const val LIGHT_TYPE_LUMINANCE = 0.55f

/** The name of a type in its color, e.g. a Fire badge */
@Composable
fun TypeBadge(type: PokeType, modifier: Modifier = Modifier) {
    val background = type.color
    Text(
        type.displayName,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = if (background.luminance() > LIGHT_TYPE_LUMINANCE) Color(0xFF1F1F1F) else Color.White,
        modifier = modifier
            .background(background, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** e.g. Fire */
val PokeType.displayName: String get() = name.lowercase().replaceFirstChar { it.uppercase() }
