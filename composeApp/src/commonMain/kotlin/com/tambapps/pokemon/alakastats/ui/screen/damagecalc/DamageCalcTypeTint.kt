package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.tambapps.pokemon.alakastats.ui.theme.color
import com.tambapps.pokemon.alakastats.ui.theme.isDarkThemeEnabled
import com.tambapps.pokemon.champions.data.ChampionsDex

private const val TYPE_TINT_LIGHT_ALPHA = 0.25f
private const val TYPE_TINT_DARK_ALPHA = 0.18f

/**
 * A wash of the pokemon's primary type color fading out towards the field: from the left for the attacker, from the
 * right for the defender
 *
 * @param diagonalLength the length of a diagonal fade from the top corner, or null for a horizontal fade over the whole
 * width (for small cards)
 */
@Composable
internal fun Modifier.typeTint(state: DamageCalcPokemonState, side: DamageCalcSide, diagonalLength: Dp? = null): Modifier {
    val type = ChampionsDex.speciesOrNull(state.form)?.primaryType ?: return this
    // lighter in dark mode, where the colors stand out more
    val tint = type.color.copy(alpha = if (isDarkThemeEnabled()) TYPE_TINT_DARK_ALPHA else TYPE_TINT_LIGHT_ALPHA)
    return drawBehind {
        val length = diagonalLength?.toPx() ?: size.width
        val startX = if (side == DamageCalcSide.ATTACKER) 0f else size.width
        val endX = if (side == DamageCalcSide.ATTACKER) length else size.width - length
        drawRect(
            Brush.linearGradient(
                colors = listOf(tint, Color.Transparent),
                start = Offset(startX, 0f),
                end = Offset(endX, if (diagonalLength != null) length else 0f),
            )
        )
    }
}
