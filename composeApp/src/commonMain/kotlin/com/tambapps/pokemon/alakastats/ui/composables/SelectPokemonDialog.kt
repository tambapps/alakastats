package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.swmansion.kmpwheelpicker.rememberWheelPickerState
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.ui.model.PokemonFilter
import com.tambapps.pokemon.alakastats.ui.service.availablePokemonNames
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact

@Composable
fun SelectPokemonDialog(
    containsValidator: (PokemonName) -> Boolean,
    asLead: Boolean,
    onAdd: (PokemonFilter) -> Unit,
    onDismissRequest: () -> Unit,
    title: String,
    confirmButtonText: String,
) {
    val allPokemons = availablePokemonNames()
    var text by remember { mutableStateOf("") }
    val pokemons = remember(text) {
        if (text.isBlank()) allPokemons
        else allPokemons.filter { it.value.contains(text, ignoreCase = true) }
    }
    var error: String? by remember { mutableStateOf(null) }
    val wheelState = rememberWheelPickerState(itemCount = pokemons.size, initialIndex = 0)
    val limit = if (LocalIsCompact.current) 3 else 10
    val showWheel = remember(pokemons) { pokemons.isNotEmpty() && pokemons.size <= limit }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    value = text,
                    onValueChange = {
                        text = it
                        if (showWheel) error = null
                    },
                    isError = error != null,
                    singleLine = true,
                    supportingText = error?.let { ({ Text(it) }) },
                    label = { Text("Pokemon Name") },
                )
                if (showWheel) {
                    val keyboardController = LocalSoftwareKeyboardController.current
                    LaunchedEffect(text) {
                        keyboardController?.hide()
                        wheelState.scrollTo(0)
                    }
                    Spacer(Modifier.height(16.dp))
                    PokemonWheelPicker(
                        modifier = Modifier.weight(1f),
                        pokemons = pokemons,
                        state = wheelState
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val pokemonName = pokemons.getOrNull(wheelState.index)
                    if (!showWheel || pokemonName == null) {
                        error = "No Pokemon was selected"
                        return@TextButton
                    }
                    if (containsValidator.invoke(pokemonName)) {
                        error = "${pokemonName.pretty} was already selected"
                        return@TextButton
                    }
                    onAdd.invoke(PokemonFilter(pokemonName, asLead))
                    onDismissRequest.invoke()
                },
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}
