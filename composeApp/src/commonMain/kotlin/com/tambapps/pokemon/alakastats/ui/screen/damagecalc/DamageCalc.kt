package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.content_copy
import alakastats.composeapp.generated.resources.swap_horiz
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.theme.isDarkThemeEnabled
import com.tambapps.pokemon.alakastats.util.copyToClipboard
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/**
 * Reusable damage calculator component, used both in the teamlytics tab and in the standalone screen
 *
 * @param team if provided, allows to fill the attacker/defender with a pokemon of this team
 * @param pagerSwipeEnabled whether the mobile pager can be swiped. Should be disabled when the component
 * is itself displayed inside a horizontal pager
 */
@Composable
fun DamageCalc(
    viewModel: DamageCalcViewModel,
    modifier: Modifier = Modifier,
    team: Teamlytics? = null,
    pagerSwipeEnabled: Boolean = true,
) {
    val isCompact = LocalIsCompact.current
    if (isCompact) {
        DamageCalcMobile(viewModel, modifier, team, pagerSwipeEnabled)
    } else {
        DamageCalcDesktop(viewModel, modifier, team)
    }
}

@Composable
internal fun DamageResultHeader(
    viewModel: DamageCalcViewModel,
    onFieldSummaryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    calcTitle(viewModel, viewModel.selectedMoveSide),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.swap() }) {
                    Icon(
                        painter = painterResource(Res.drawable.swap_horiz),
                        contentDescription = "Swap attacker and defender"
                    )
                }
            }
            when (val result = viewModel.result) {
                is DamageCalcResult.Success -> {
                    Text(
                        "${result.damageRangeText} (${result.damagePercentText})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    DamageRangeBar(
                        currentHpFraction = result.currentHpFraction,
                        damageFractionRange = result.damageFractionRange,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                    Text(result.koChanceText, style = MaterialTheme.typography.bodyMedium)
                }
                is DamageCalcResult.Error -> Text(
                    result.message,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    Text(
                        viewModel.field.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onFieldSummaryClick)
                    )
                }
                (viewModel.result as? DamageCalcResult.Success)?.let { CopyCalcButton(it) }
            }
        }
    }
}

// e.g. "Mega Charizard X → Incineroar", from the pokemon using the move on [side] to its target
internal fun calcTitle(viewModel: DamageCalcViewModel, side: DamageCalcSide) =
    "${viewModel.pokemonState(side).form.pretty} → ${viewModel.pokemonState(side.opponent).form.pretty}"

@Composable
internal fun CopyCalcButton(result: DamageCalcResult.Success) {
    val snackbar = LocalSnackBar.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    IconButton(onClick = {
        scope.launch {
            if (copyToClipboard(clipboard, label = "Damage calc", text = result.description)) {
                snackbar.show("Copied to clipboard")
            } else {
                snackbar.show("Copy to clipboard not supported")
            }
        }
    }) {
        Icon(
            painter = painterResource(Res.drawable.content_copy),
            contentDescription = "Copy calc to clipboard"
        )
    }
}

/**
 * The defender's HP bar after one use of the move, from left to right:
 * - green: the HP left even after the highest roll
 * - orange: the damage range, the HP left depending on the roll
 * - red: the HP surely lost to the move (its lowest roll)
 * - grey: the HP already missing before the move (when the current HP isn't full)
 *
 * @param currentHpFraction the defender's current HP, as a fraction of its max HP
 * @param damageFractionRange the damage of one use of the move, as fractions of the defender's max HP
 */
@Composable
internal fun DamageRangeBar(
    currentHpFraction: Float,
    damageFractionRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    val missingHpColor = MaterialTheme.colorScheme.surfaceVariant
    val colors = damageBarColors()
    Canvas(modifier.height(8.dp)) {
        val cornerRadius = CornerRadius(size.height / 2)
        fun drawUpTo(fraction: Float, color: Color) = drawRoundRect(
            color,
            size = Size(size.width * fraction.coerceIn(0f, 1f), size.height),
            cornerRadius = cornerRadius
        )
        // each part is drawn from the left over the wider ones, so the visible part of each is what's past the next one
        drawRoundRect(missingHpColor, cornerRadius = cornerRadius)
        drawUpTo(currentHpFraction, colors.surelyLost)
        drawUpTo(currentHpFraction - damageFractionRange.start, colors.dependsOnRoll)
        drawUpTo(currentHpFraction - damageFractionRange.endInclusive, colors.alwaysLeft)
    }
}

private class DamageBarColors(val alwaysLeft: Color, val dependsOnRoll: Color, val surelyLost: Color)

// with clearly different brightness, so that the parts stay distinct for red-green colorblind players
@Composable
private fun damageBarColors() = if (isDarkThemeEnabled()) {
    DamageBarColors(alwaysLeft = Color(0xFF66BB6A), dependsOnRoll = Color(0xFFFFB74D), surelyLost = Color(0xFFE53935))
} else {
    DamageBarColors(alwaysLeft = Color(0xFF4CAF50), dependsOnRoll = Color(0xFFFFA726), surelyLost = Color(0xFFC62828))
}

@Composable
internal fun MoveChips(viewModel: DamageCalcViewModel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val attacker = viewModel.attacker
        attacker.moves.forEachIndexed { index, move ->
            // empty move slots are not selectable
            if (move.isBlank()) return@forEachIndexed
            val hits = attacker.hitCount(index)
            FilterChip(
                selected = viewModel.selectedMoveSide == DamageCalcSide.ATTACKER && viewModel.selectedMoveIndex == index,
                onClick = { viewModel.selectMove(DamageCalcSide.ATTACKER, index) },
                label = { Text(if (hits > 1) "$move ×$hits" else move) }
            )
        }
    }
}
