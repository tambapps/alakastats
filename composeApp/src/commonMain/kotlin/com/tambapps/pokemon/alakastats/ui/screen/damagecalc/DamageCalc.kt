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
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.util.copyToClipboard
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/**
 * Reusable damage calculator component, used both in the teamlytics tab and in the standalone screen
 *
 * @param pagerSwipeEnabled whether the mobile pager can be swiped. Should be disabled when the component
 * is itself displayed inside a horizontal pager
 */
@Composable
fun DamageCalc(
    viewModel: DamageCalcViewModel,
    modifier: Modifier = Modifier,
    pagerSwipeEnabled: Boolean = true,
) {
    val isCompact = LocalIsCompact.current
    if (isCompact) {
        DamageCalcMobile(viewModel, modifier, pagerSwipeEnabled)
    } else {
        DamageCalcDesktop(viewModel, modifier)
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
                    "${viewModel.attacker.name.pretty} → ${viewModel.defender.name.pretty}",
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
            Text("${viewModel.damageRangeText} (${viewModel.damagePercentText})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            DamageRangeBar(viewModel.damagePercentRange, Modifier.fillMaxWidth().padding(vertical = 4.dp))
            Text(viewModel.koChanceText, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    Text(
                        viewModel.field.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onFieldSummaryClick)
                    )
                }
                CopyCalcButton(viewModel)
            }
        }
    }
}

@Composable
private fun CopyCalcButton(viewModel: DamageCalcViewModel) {
    val snackbar = LocalSnackBar.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    IconButton(onClick = {
        scope.launch {
            if (copyToClipboard(clipboard, label = "Damage calc", text = viewModel.calcDescription)) {
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

@Composable
private fun DamageRangeBar(damagePercentRange: ClosedFloatingPointRange<Float>, modifier: Modifier = Modifier) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val damageRangeColor = MaterialTheme.colorScheme.error
    val remainingHpColor = MaterialTheme.colorScheme.primary
    Canvas(modifier.height(8.dp)) {
        val cornerRadius = CornerRadius(size.height / 2)
        drawRoundRect(trackColor, cornerRadius = cornerRadius)
        // HP left after the lowest roll, which includes the damage range
        drawRoundRect(
            damageRangeColor,
            size = Size(size.width * (1f - damagePercentRange.start).coerceIn(0f, 1f), size.height),
            cornerRadius = cornerRadius
        )
        // HP left after the highest roll
        drawRoundRect(
            remainingHpColor,
            size = Size(size.width * (1f - damagePercentRange.endInclusive).coerceIn(0f, 1f), size.height),
            cornerRadius = cornerRadius
        )
    }
}

@Composable
internal fun MoveChips(viewModel: DamageCalcViewModel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        viewModel.attacker.moves.forEachIndexed { index, move ->
            FilterChip(
                selected = viewModel.selectedMoveIndex == index,
                onClick = { viewModel.selectedMoveIndex = index },
                label = { Text(move) }
            )
        }
    }
}
