package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.replay

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
import com.tambapps.pokemon.alakastats.ui.model.ReplaySort
import com.tambapps.pokemon.alakastats.ui.model.ReplaySortCriterion
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import org.jetbrains.compose.resources.painterResource

/**
 * Chip displaying the order of the replays, opening a menu to pick the criterion and the direction. It is selected when
 * the order isn't the default one (newest first)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReplaySortChip(
    sort: ReplaySort,
    onSortChange: (ReplaySort) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val isCompact = LocalIsCompact.current

    // the box anchors the menu under the chip
    Box(modifier) {
        FilterChip(
            onClick = { expanded = true },
            selected = !sort.isDefault,
            leadingIcon = {
                Icon(
                    painter = painterResource(Res.drawable.arrow_forward),
                    contentDescription = sort.directionLabel,
                    // pointing down for the descending order
                    modifier = Modifier.size(FilterChipDefaults.IconSize).rotate(if (sort.descending) 90f else -90f),
                )
            },
            label = {
                // the direction is told by the arrow on mobile, where the bar has less room
                Text(
                    if (isCompact) sort.criterion.label else "${sort.criterion.label} · ${sort.directionLabel}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MenuSectionTitle("Sort by")
            ReplaySortCriterion.entries.forEach { criterion ->
                SortMenuItem(
                    text = criterion.label,
                    selected = criterion == sort.criterion,
                    onClick = {
                        // a new criterion starts in its natural direction
                        if (criterion != sort.criterion) onSortChange(ReplaySort(criterion))
                        expanded = false
                    }
                )
            }
            HorizontalDivider()
            MenuSectionTitle("Order")
            listOf(true, false).forEach { descending ->
                SortMenuItem(
                    text = if (descending) sort.criterion.descendingLabel else sort.criterion.ascendingLabel,
                    selected = descending == sort.descending,
                    onClick = {
                        onSortChange(sort.copy(descending = descending))
                        expanded = false
                    }
                )
            }
            if (!sort.isDefault) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Reset to newest first") },
                    onClick = {
                        onSortChange(ReplaySort())
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
