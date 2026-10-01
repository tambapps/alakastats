package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.ui.model.PokemonFilter
import com.tambapps.pokemon.alakastats.ui.service.availablePokemonNames
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.pokepaste.parser.PokePasteParseException
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import org.koin.compose.koinInject

private const val POKEPASTE_MAX_LINES = 10

@Composable
fun SelectPokemonDialog(
    containsValidator: (PokemonName) -> Boolean,
    asLead: Boolean,
    onAdd: (PokemonFilter) -> Unit,
    onDismissRequest: () -> Unit,
    title: String,
    confirmButtonText: String,
) = SelectPokemonDialog(
    onSelect = { onAdd.invoke(PokemonFilter(it, asLead)) },
    onDismissRequest = onDismissRequest,
    title = title,
    confirmButtonText = confirmButtonText,
    containsValidator = containsValidator,
)

@Composable
fun SelectPokemonDialog(
    onSelect: (PokemonName) -> Unit,
    onDismissRequest: () -> Unit,
    title: String,
    confirmButtonText: String,
    containsValidator: (PokemonName) -> Boolean = { false },
    // optional extra button, displayed before the cancel button
    extraButton: (@Composable () -> Unit)? = null,
    // the pokemons that can be selected
    allPokemons: List<PokemonName> = availablePokemonNames(),
    // if set, the text field is multiline and also accepts a pokepaste, whose first pokemon is selected
    onPokepasteSelect: ((Pokemon) -> Unit)? = null,
    // to replace the text field's default label
    label: String? = null,
) {
    var text by remember { mutableStateOf("") }
    // more than one line: a pokepaste
    val isPokepaste = onPokepasteSelect != null && text.trim().contains('\n')
    val pokemons = remember(text, allPokemons) {
        if (isPokepaste) emptyList()
        else if (text.isBlank()) allPokemons
        else allPokemons.filter { it.value.contains(text.trim(), ignoreCase = true) }
    }
    val pokepasteParser = koinInject<PokepasteParser>()
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
                        if (showWheel || isPokepaste) error = null
                    },
                    isError = error != null,
                    singleLine = onPokepasteSelect == null,
                    maxLines = if (onPokepasteSelect == null) 1 else POKEPASTE_MAX_LINES,
                    supportingText = error?.let { ({ Text(it) }) },
                    label = {
                        Text(label ?: if (onPokepasteSelect == null) "Pokemon Name" else "Pokemon Name or Pokepaste")
                    },
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
                    if (isPokepaste) {
                        try {
                            onPokepasteSelect?.invoke(pokepasteParser.parse(text).pokemons.first())
                            onDismissRequest.invoke()
                        } catch (e: PokePasteParseException) {
                            error = "Invalid pokepaste: ${e.message}"
                        }
                        return@TextButton
                    }
                    val pokemonName = pokemons.getOrNull(wheelState.index)
                    if (!showWheel || pokemonName == null) {
                        error = "No Pokemon was selected"
                        return@TextButton
                    }
                    if (containsValidator.invoke(pokemonName)) {
                        error = "${pokemonName.pretty} was already selected"
                        return@TextButton
                    }
                    onSelect.invoke(pokemonName)
                    onDismissRequest.invoke()
                },
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            Row {
                extraButton?.invoke()
                TextButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
            }
        }
    )
}
