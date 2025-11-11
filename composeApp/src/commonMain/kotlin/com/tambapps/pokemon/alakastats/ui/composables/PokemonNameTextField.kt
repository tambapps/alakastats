package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService

@Composable
fun PokemonNameTextField(
    value: PokemonName,
    placeholder: String = "Pokemon",
    onValueChange: (PokemonName) -> Unit,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    pokemonImageService: PokemonImageService,
) {
    var expanded by remember { mutableStateOf(false) }

    var textFieldValue by remember(value) {
        mutableStateOf(TextFieldValue(
            text = value.value,
            selection = TextRange(value.value.length)
        ))
    }

    val allNames = remember { pokemonImageService.listAvailableNames() }
    val filteredSuggestions = remember(value) {
        val formattedValue = value.normalized
        if (formattedValue.value.isEmpty()) allNames
        else allNames.filter { it.value.startsWith(formattedValue.value) }
    }

    Box(modifier = modifier) {
        var textFieldWidth by remember { mutableStateOf(0.dp) }
        val density = LocalDensity.current

        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                if (!expanded && newValue.text.length > textFieldValue.text.length) {
                    expanded = newValue.text.length >= 4 || filteredSuggestions.size < 5
                }
                textFieldValue = newValue
                onValueChange(PokemonName(newValue.text))
            },
            isError = isError,
            supportingText = supportingText,
            modifier = Modifier.fillMaxWidth()
                .onSizeChanged { size ->
                    with(density) { textFieldWidth = size.width.toDp() }
                },
            label = { Text(placeholder) },
        )

        DropdownMenu(
            expanded = expanded && filteredSuggestions.isNotEmpty(),
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(textFieldWidth)
        ) {
            filteredSuggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pokemonImageService.PokemonSprite(suggestion, Modifier.size(40.dp), disableTooltip = true)
                            Spacer(Modifier.width(8.dp))
                            Text(suggestion.pretty)
                        }
                    },
                    onClick = {
                        val newText = suggestion.pretty
                        textFieldValue = TextFieldValue(
                            text = newText,
                            selection = TextRange(newText.length)
                        )
                        onValueChange(PokemonName(newText))
                        expanded = false
                    }
                )
            }
        }
    }
}
