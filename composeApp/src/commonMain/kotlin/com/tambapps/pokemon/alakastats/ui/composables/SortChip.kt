package com.tambapps.pokemon.alakastats.ui.composables

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.arrow_forward
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.model.SortCriterion
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import org.jetbrains.compose.resources.painterResource

/**
 * Chip displaying the order of a list, opening a menu to pick the criterion and the direction. It is selected when the
 * order isn't the default one
 *
 * @param onSortChange called with the picked criterion and direction, a new criterion starting in its natural direction
 * @param resetLabel what the item going back to the default order tells
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <C : SortCriterion> SortChip(
    criteria: List<C>,
    criterion: C,
    descending: Boolean,
    isDefault: Boolean,
    onSortChange: (criterion: C, descending: Boolean) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    resetLabel: String = "Reset to newest first",
) {
    var expanded by remember { mutableStateOf(false) }
    val isCompact = LocalIsCompact.current
    val directionLabel = if (descending) criterion.descendingLabel else criterion.ascendingLabel

    // the box anchors the menu under the chip
    Box(modifier) {
        FilterChip(
            onClick = { expanded = true },
            selected = !isDefault,
            leadingIcon = {
                Icon(
                    painter = painterResource(Res.drawable.arrow_forward),
                    contentDescription = directionLabel,
                    // pointing down for the descending order
                    modifier = Modifier.size(FilterChipDefaults.IconSize).rotate(if (descending) 90f else -90f),
                )
            },
            label = {
                // the direction is told by the arrow on mobile, where the bar has less room
                Text(
                    if (isCompact) criterion.label else "${criterion.label} · $directionLabel",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MenuSectionTitle("Sort by")
            criteria.forEach { entry ->
                SortMenuItem(
                    text = entry.label,
                    selected = entry == criterion,
                    onClick = {
                        // a new criterion starts in its natural direction
                        if (entry != criterion) onSortChange(entry, entry.defaultDescending)
                        expanded = false
                    }
                )
            }
            HorizontalDivider()
            MenuSectionTitle("Order")
            listOf(true, false).forEach { entryDescending ->
                SortMenuItem(
                    text = if (entryDescending) criterion.descendingLabel else criterion.ascendingLabel,
                    selected = entryDescending == descending,
                    onClick = {
                        onSortChange(criterion, entryDescending)
                        expanded = false
                    }
                )
            }
            if (!isDefault) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(resetLabel) },
                    onClick = {
                        onReset()
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MenuSectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun SortMenuItem(text: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, fontWeight = if (selected) FontWeight.Bold else null) },
        leadingIcon = { RadioButton(selected = selected, onClick = null) },
        onClick = onClick,
    )
}
