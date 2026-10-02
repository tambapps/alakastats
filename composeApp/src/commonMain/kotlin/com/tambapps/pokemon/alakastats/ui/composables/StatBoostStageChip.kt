package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

data class StatBoostStage(val level: Int, val multiplier: Float) {

    val levelText get() = if (level >= 0) "+$level" else level.toString()

    override fun toString() = buildString {
        if (level >= 0) append("+")
        append(level)
        append(" (x")
        append(if (multiplier % 1.0 == 0.0) multiplier.toInt() else multiplier)
        append(")")
    }
}

val STAT_BOOST_STAGES = listOf(
    StatBoostStage(6, 4.0f),
    StatBoostStage(5, 3.5f),
    StatBoostStage(4, 3.0f),
    StatBoostStage(3, 2.5f),
    StatBoostStage(2, 2.0f),
    StatBoostStage(1, 1.5f),
    StatBoostStage(0, 1.0f),
    StatBoostStage(-1, 0.67f),
    StatBoostStage(-2, 0.5f),
    StatBoostStage(-3, 0.4f),
    StatBoostStage(-4, 0.33f),
    StatBoostStage(-5, 0.29f),
    StatBoostStage(-6, 0.25f)
)
private const val NEUTRAL_STAT_BOOST_STAGE_INDEX = 6

/**
 * Chip displaying a stat boost stage, opening a wheel picker to select another one when clicked
 *
 * @param compact whether to only display the stage level (e.g. +1) instead of the level and the multiplier
 * @param useMenu whether to select the stage in a dropdown menu under the chip instead of a wheel picker dialog, e.g.
 * on desktop
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatBoostStageChip(
    stage: Int,
    onValueChange: (Int) -> Unit,
    dialogTitle: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(),
    useMenu: Boolean = false,
) {
    var show by remember { mutableStateOf(false) }
    val stageIndex = remember(stage) {
        STAT_BOOST_STAGES.indexOfFirst { it.level == stage }.let { if (it != -1) it else NEUTRAL_STAT_BOOST_STAGE_INDEX }
    }

    // the box anchors the menu under the chip
    Box {
        FilterChip(
            modifier = modifier,
            onClick = { show = true },
            label = {
                if (compact) {
                    Text(STAT_BOOST_STAGES[stageIndex].levelText, textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
                } else {
                    Text(STAT_BOOST_STAGES[stageIndex].toString(), textAlign = TextAlign.Center)
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = show)
                }
            },
            selected = stage != 0,
            colors = colors,
        )
        if (useMenu) {
            DropdownMenu(expanded = show, onDismissRequest = { show = false }) {
                STAT_BOOST_STAGES.forEachIndexed { index, boostStage ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                boostStage.toString(),
                                fontWeight = if (index == stageIndex) FontWeight.Bold else null,
                            )
                        },
                        onClick = {
                            onValueChange(boostStage.level)
                            show = false
                        },
                    )
                }
            }
        }
    }

    if (show && !useMenu) {
        WheelPickerDialog(
            title = dialogTitle,
            items = STAT_BOOST_STAGES,
            initialIndex = stageIndex,
            onPicked = { onValueChange.invoke(it.level) },
            onDismissRequest = { show = false },
        )
    }
}
