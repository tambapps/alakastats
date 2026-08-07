package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.Stat
import com.tambapps.pokemon.alakastats.ui.screen.home.buttonTextStyle
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.theme.isDarkThemeEnabled

object NatureQuizSetupScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<NatureQuizSetupViewModel>()
        val isCompact = LocalIsCompact.current

        if (isCompact) {
            NatureQuizSetupScreenMobile(viewModel)
        } else {
            NatureQuizSetupScreenDesktop(viewModel)
        }

        if (viewModel.showIgnoredNaturesDialog) {
            IgnoredNaturesDialog(viewModel)
        }
    }
}

private val statOrder = listOf(Stat.ATTACK, Stat.DEFENSE, Stat.SPECIAL_ATTACK, Stat.SPECIAL_DEFENSE, Stat.SPEED)
private val diagonalNatures = listOf(Nature.HARDY, Nature.DOCILE, Nature.BASHFUL, Nature.QUIRKY, Nature.SERIOUS)

private fun natureFor(increasedStat: Stat, decreasedStat: Stat): Nature =
    if (increasedStat == decreasedStat) {
        diagonalNatures[statOrder.indexOf(increasedStat)]
    } else {
        Nature.entries.first { it.bonusStat == increasedStat && it.malusStat == decreasedStat }
    }

private val compactCellWidth = 84.dp
private val expandedCellWidth = 128.dp
private val compactCellHeight = 66.dp
private val expandedCellHeight = 64.dp
private val compactSpacing = 6.dp
private val expandedSpacing = 10.dp

@Composable
internal fun NatureGrid(viewModel: NatureQuizSetupViewModel, modifier: Modifier = Modifier) {
    val isCompact = LocalIsCompact.current
    val increased = increasedStatColor
    val decreased = decreasedStatColor
    val cellWidth = if (isCompact) compactCellWidth else expandedCellWidth
    val cellHeight = if (isCompact) compactCellHeight else expandedCellHeight
    val spacing = if (isCompact) compactSpacing else expandedSpacing
    val headerStyle = if (isCompact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall
    val nameStyle = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.titleMedium

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            Box(Modifier.size(cellWidth, cellHeight))
            for (rowStat in statOrder) {
                AxisHeaderCell("↑ ${rowStat.shortLabel}", increased, cellWidth, cellHeight, headerStyle)
            }
        }
        for (colStat in statOrder) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                AxisHeaderCell("↓ ${colStat.shortLabel}", decreased, cellWidth, cellHeight, headerStyle)
                for (rowStat in statOrder) {
                    val nature = natureFor(rowStat, colStat)
                    NatureCell(
                        nature = nature,
                        isNeutral = rowStat == colStat,
                        isIgnored = nature in viewModel.ignoredNatures,
                        showStats = isCompact,
                        width = cellWidth,
                        height = cellHeight,
                        nameStyle = nameStyle,
                        onClick = { viewModel.toggleIgnored(nature) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AxisHeaderCell(text: String, textColor: Color, width: Dp, height: Dp, style: TextStyle) {
    Box(
        modifier = Modifier.size(width, height),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = textColor, fontWeight = FontWeight.Bold, style = style, textAlign = TextAlign.Center)
    }
}

@Composable
private fun NatureCell(
    nature: Nature,
    isNeutral: Boolean,
    isIgnored: Boolean,
    showStats: Boolean,
    width: Dp,
    height: Dp,
    nameStyle: TextStyle,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isIgnored -> Color.Transparent
        isNeutral -> MaterialTheme.colorScheme.inverseSurface
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (isNeutral && !isIgnored) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(width, height)
            .clip(shape)
            .background(backgroundColor)
            .then(
                if (isIgnored) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape) else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                nature.displayName,
                color = contentColor,
                textAlign = TextAlign.Center,
                style = nameStyle
            )
            if (showStats && !isNeutral) {
                Text(
                    "+${nature.bonusStat!!.abbreviation}",
                    color = increasedStatColor,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
internal fun IgnoreNaturesButton(viewModel: NatureQuizSetupViewModel, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = { viewModel.openIgnoredNaturesDialog() }, modifier = modifier) {
        Text(
            "Ignored natures (${viewModel.ignoredNatures.size})",
            style = buttonTextStyle,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun StartQuizButton(viewModel: NatureQuizSetupViewModel, modifier: Modifier = Modifier) {
    Button(
        onClick = { viewModel.startQuiz() },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
    ) {
        Text("Start", style = buttonTextStyle.copy(color = LocalContentColor.current))
    }
}

@Composable
private fun IgnoredNaturesDialog(viewModel: NatureQuizSetupViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.cancelIgnoredNaturesDialog() },
        title = { Text("Ignored natures") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                for (nature in viewModel.natures) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleIgnored(nature) }
                    ) {
                        Checkbox(
                            checked = nature in viewModel.ignoredNatures,
                            onCheckedChange = { viewModel.toggleIgnored(nature) }
                        )
                        Text(nature.displayName, modifier = Modifier.weight(1f))
                        NatureStatsCaption(nature)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { viewModel.confirmIgnoredNaturesDialog() }) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.cancelIgnoredNaturesDialog() }) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun NatureStatsCaption(nature: Nature) {
    if (nature.isNeutral) {
        Text("Neutral", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        Row {
            Text("+${nature.bonusStat!!.abbreviation}", color = increasedStatColor, style = MaterialTheme.typography.bodySmall)
            Text(" / ", style = MaterialTheme.typography.bodySmall)
            Text("-${nature.malusStat!!.abbreviation}", color = decreasedStatColor, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private val increasedStatColor @Composable get() = if (isDarkThemeEnabled()) Color(0xFFE57373) else Color.Red
private val decreasedStatColor @Composable get() = if (isDarkThemeEnabled()) Color.Cyan else Color.Blue

private val Nature.displayName: String
    get() = name.lowercase().replaceFirstChar { it.uppercase() }

private val Stat.shortLabel: String
    get() = when (this) {
        Stat.ATTACK -> "Attack"
        Stat.DEFENSE -> "Defense"
        Stat.SPECIAL_ATTACK -> "Sp. Atk"
        Stat.SPECIAL_DEFENSE -> "Sp. Def"
        Stat.SPEED -> "Speed"
        Stat.HP -> "HP"
    }

private val Stat.abbreviation: String
    get() = when (this) {
        Stat.ATTACK -> "Atk"
        Stat.DEFENSE -> "Def"
        Stat.SPECIAL_ATTACK -> "SpAtk"
        Stat.SPECIAL_DEFENSE -> "SpDef"
        Stat.SPEED -> "Spe"
        Stat.HP -> "HP"
    }
