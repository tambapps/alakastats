package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.Stat
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.decreasedStatColor
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.displayName
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.increasedStatColor
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.statOrder

/**
 * The stat abbreviations of the nature picker, short enough for the grid's cells and the closed field
 */
private val Stat.natureAbbreviation: String
    get() = when (this) {
        Stat.ATTACK -> "Atk"
        Stat.DEFENSE -> "Def"
        Stat.SPECIAL_ATTACK -> "SpA"
        Stat.SPECIAL_DEFENSE -> "SpD"
        Stat.SPEED -> "Spe"
        Stat.HP -> "HP"
    }

// the neutral natures all behave the same, the grid picks this one for them
private val NEUTRAL_NATURE = Nature.SERIOUS

/**
 * The nature's name, then its boosted stat in red and its lowered one in blue (or "Neutral")
 */
@Composable
internal fun natureText(nature: Nature): AnnotatedString {
    val increased = increasedStatColor
    val decreased = decreasedStatColor
    val neutral = MaterialTheme.colorScheme.onSurfaceVariant
    return buildAnnotatedString {
        append(nature.displayName)
        append("  ")
        val bonusStat = nature.bonusStat
        val malusStat = nature.malusStat
        if (nature.isNeutral || bonusStat == null || malusStat == null) {
            withStyle(SpanStyle(color = neutral)) { append("Neutral") }
        } else {
            withStyle(SpanStyle(color = increased, fontWeight = FontWeight.Bold)) {
                append("+${bonusStat.natureAbbreviation}")
            }
            append(" ")
            withStyle(SpanStyle(color = decreased, fontWeight = FontWeight.Bold)) {
                append("−${malusStat.natureAbbreviation}")
            }
        }
    }
}

/**
 * The natures by the stat they boost (rows, in red) and the one they lower (columns, in blue). The diagonal is the
 * neutral natures, all picking [NEUTRAL_NATURE]
 */
@Composable
internal fun NatureGrid(
    selected: Nature,
    onSelect: (Nature) -> Unit,
    cellHeight: Dp,
    nameStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val increased = increasedStatColor
    val decreased = decreasedStatColor
    val headerStyle = nameStyle.copy(fontWeight = FontWeight.Bold)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CELL_SPACING)) {
        Row(horizontalArrangement = Arrangement.spacedBy(CELL_SPACING)) {
            Box(Modifier.weight(HEADER_WEIGHT))
            statOrder.forEach { stat ->
                HeaderCell("−${stat.natureAbbreviation}", decreased, headerStyle, cellHeight)
            }
        }
        statOrder.forEach { bonusStat ->
            Row(horizontalArrangement = Arrangement.spacedBy(CELL_SPACING)) {
                HeaderCell("+${bonusStat.natureAbbreviation}", increased, headerStyle, cellHeight, Modifier.weight(HEADER_WEIGHT))
                statOrder.forEach { malusStat ->
                    val nature = if (bonusStat == malusStat) null
                    else Nature.entries.first { it.bonusStat == bonusStat && it.malusStat == malusStat }
                    NatureCell(
                        nature = nature,
                        // all the diagonal is selected for a neutral nature, the 5 of them being the same
                        isSelected = if (nature == null) selected.isNeutral else nature == selected,
                        onClick = { onSelect(nature ?: NEUTRAL_NATURE) },
                        nameStyle = nameStyle,
                        height = cellHeight,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.HeaderCell(
    text: String,
    color: Color,
    style: TextStyle,
    height: Dp,
    modifier: Modifier = Modifier.weight(1f),
) {
    Box(modifier.height(height), contentAlignment = Alignment.Center) {
        Text(text, color = color, style = style, maxLines = 1, softWrap = false)
    }
}

/**
 * A nature of the grid, null for a neutral one
 */
@Composable
private fun RowScope.NatureCell(
    nature: Nature?,
    isSelected: Boolean,
    onClick: () -> Unit,
    nameStyle: TextStyle,
    height: Dp,
) {
    val colors = MaterialTheme.colorScheme
    val background = when {
        isSelected -> colors.primary
        nature == null -> colors.surfaceContainer
        else -> colors.surfaceContainerHighest
    }
    val textColor = when {
        isSelected -> colors.onPrimary
        nature == null -> colors.onSurfaceVariant
        else -> colors.onSurface
    }
    Box(
        Modifier.weight(1f)
            .height(height)
            .clip(CELL_SHAPE)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            nature?.displayName ?: "Neutral",
            color = textColor,
            style = nameStyle,
            fontWeight = if (isSelected) FontWeight.Bold else null,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }
}

private val CELL_SPACING = 4.dp
private val CELL_SHAPE = RoundedCornerShape(6.dp)
private const val HEADER_WEIGHT = 0.8f

/**
 * The nature field of the desktop layout, opening the grid in a menu under it
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NatureField(nature: Nature, onSelect: (Nature) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = TextFieldValue(natureText(nature)),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Nature") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth(),
        )
        // the read-only text field doesn't open the menu by itself
        Box(Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            NatureGrid(
                selected = nature,
                onSelect = {
                    onSelect(it)
                    expanded = false
                },
                cellHeight = 40.dp,
                nameStyle = MaterialTheme.typography.labelLarge,
                modifier = Modifier.width(DESKTOP_GRID_WIDTH).padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

private val DESKTOP_GRID_WIDTH = 520.dp

/**
 * The nature grid of the mobile layout, in a dialog as wide as the screen allows
 */
@Composable
internal fun NatureGridDialog(nature: Nature, onSelect: (Nature) -> Unit, onDismissRequest: () -> Unit) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Select Nature",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                )
                NatureGrid(
                    selected = nature,
                    onSelect = {
                        onSelect(it)
                        onDismissRequest()
                    },
                    cellHeight = 44.dp,
                    nameStyle = MaterialTheme.typography.labelSmall,
                )
                TextButton(onClick = onDismissRequest, modifier = Modifier.align(Alignment.End)) {
                    Text("Cancel")
                }
            }
        }
    }
}
