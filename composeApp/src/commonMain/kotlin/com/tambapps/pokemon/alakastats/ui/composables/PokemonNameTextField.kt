package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable



import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.tambapps.pokemon.PokemonName

@Composable
fun PokemonNameTextField(
    value: PokemonName,
    placeholder: String = "Pokemon",
    onValueChange: (PokemonName) -> Unit,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val allNames = remember { listOf<PokemonName>(
        PokemonName("baxcalibur")
    ) }
    val filteredSuggestions = remember(value) {
        val formattedValue = value.normalized
        if (formattedValue.value.isEmpty()) allNames
        else allNames.filter { it.value.startsWith(formattedValue.value) }
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value.value,
            onValueChange = {
                onValueChange(PokemonName(it))
                expanded = it.isNotEmpty()
            },
            isError = isError,
            supportingText = supportingText,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(placeholder) },

        )

        DropdownMenu(
            expanded = expanded && filteredSuggestions.isNotEmpty(),
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            filteredSuggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AsyncImage(
                                model = "https://i.redd.it/hwyb-baxcalibur-from-pokemon-v0-qlenoi5l747a1.jpg?width=1130&format=pjpg&auto=webp&s=a9f419c2a606025684304b6789d02b1882a305cb",
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(suggestion.value)
                        }
                    },
                    onClick = {
                        onValueChange(suggestion)
                        expanded = false
                    }
                )
            }
        }
    }
}
