package com.tambapps.pokemon.alakastats.ui.composables

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.check
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokeType
import org.jetbrains.compose.resources.painterResource

/**
 * A chip filtering by a type, picked from a menu of the types, null for all of them
 *
 * @param types the types the menu proposes, by name
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeFilterChip(
    type: PokeType?,
    onTypeChange: (PokeType?) -> Unit,
    modifier: Modifier = Modifier,
    types: Collection<PokeType> = PokeType.entries,
) {
    var expanded by remember { mutableStateOf(false) }
    // the box anchors the menu under the chip
    Box(modifier) {
        FilterChip(
            selected = type != null,
            onClick = { expanded = true },
            label = { if (type != null) TypeBadge(type) else Text("Type", maxLines = 1) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("All types", fontWeight = if (type == null) FontWeight.Bold else null) },
                onClick = {
                    onTypeChange(null)
                    expanded = false
                },
            )
            HorizontalDivider()
            types.sortedBy { it.displayName }.forEach { entry ->
                DropdownMenuItem(
                    text = { TypeBadge(entry) },
                    trailingIcon = if (entry == type) {
                        { Icon(painterResource(Res.drawable.check), contentDescription = "Selected", Modifier.size(18.dp)) }
                    } else null,
                    onClick = {
                        onTypeChange(entry)
                        expanded = false
                    },
                )
            }
        }
    }
}
