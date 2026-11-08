package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swmansion.kmpwheelpicker.WheelPicker
import com.swmansion.kmpwheelpicker.WheelPickerState
import com.swmansion.kmpwheelpicker.rememberWheelPickerState
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.ui.service.PokemonSprite
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val BUFFER_SIZE = 3

/**
 * On desktop, Enter confirms the wheel's item and Up/Down move the wheel (the state keeps the index in range).
 * Returns whether the key event was used, for [onPreviewKeyEvent]
 */
@Composable
fun wheelPickerKeyHandler(state: WheelPickerState, onConfirm: () -> Unit): (KeyEvent) -> Boolean {
    val isCompact = LocalIsCompact.current
    val scope = rememberCoroutineScope()
    return handler@{ event ->
        if (isCompact || event.type != KeyEventType.KeyDown) return@handler false
        when (event.key) {
            // Shift+Enter is left to e.g. a multiline text field
            Key.Enter, Key.NumPadEnter -> if (event.isShiftPressed) false else { onConfirm(); true }
            Key.DirectionUp -> { scope.launch { state.animateScrollTo(state.index - 1) }; true }
            Key.DirectionDown -> { scope.launch { state.animateScrollTo(state.index + 1) }; true }
            else -> false
        }
    }
}

/**
 * Makes a dialog without text field receive the keys of [wheelPickerKeyHandler]: focused when shown, on desktop
 */
@Composable
fun Modifier.wheelPickerKeys(state: WheelPickerState, onConfirm: () -> Unit): Modifier {
    val focusRequester = remember { FocusRequester() }
    val isCompact = LocalIsCompact.current
    LaunchedEffect(isCompact) { if (!isCompact) focusRequester.requestFocus() }
    return focusRequester(focusRequester)
        .onPreviewKeyEvent(wheelPickerKeyHandler(state, onConfirm))
        .focusable()
}

@Composable
fun PokemonWheelPicker(
    pokemons: List<PokemonName>,
    modifier: Modifier = Modifier,
    state: WheelPickerState = rememberWheelPickerState(itemCount = pokemons.size, initialIndex = 0),
) {
    Box(modifier.clipToBounds()) {
        WheelPicker(
            state = state,
            bufferSize = BUFFER_SIZE,
            window = {
                Box(
                    Modifier.background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        MaterialTheme.shapes.small,
                    )
                )
            }
        ) { index ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier.padding(32.dp, 8.dp).graphicsLayer {
                        alpha = (BUFFER_SIZE - abs(state.value - index)).coerceIn(0f, 1f)
                    },
            ) {
                val pokemonName = pokemons[index]
                PokemonSprite(
                    pokemonName,
                    Modifier.size(if (LocalIsCompact.current) 64.dp else 128.dp)
                )
                Text(
                    text = pokemonName.pretty,
                    style = MaterialTheme.typography.headlineMedium,
                    color =
                        lerp(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                            abs(state.value - index).coerceIn(0f, 1f),
                        ),
                )

            }
        }
    }
}

/**
 * Dialog to pick one of [pokemons] with a [PokemonWheelPicker], displaying each pokemon's sprite next to its name
 */
@Composable
fun PokemonWheelPickerDialog(
    title: String,
    pokemons: List<PokemonName>,
    onPicked: (index: Int) -> Unit,
    onDismissRequest: () -> Unit,
    initialIndex: Int = 0,
) {
    val state = rememberWheelPickerState(itemCount = pokemons.size, initialIndex = initialIndex)
    val confirm = {
        if (state.index in pokemons.indices) onPicked.invoke(state.index)
        onDismissRequest.invoke()
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = { PokemonWheelPicker(pokemons = pokemons, state = state, modifier = Modifier.wheelPickerKeys(state, confirm)) },
        confirmButton = {
            TextButton(onClick = confirm) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun <T> WheelPickerDialog(
    title: String,
    items: List<T>,
    onPicked: (T) -> Unit,
    onDismissRequest: () -> Unit,
    initialIndex: Int = 0,
    itemToText: (T) -> String = { it.toString() },
    textAlign: TextAlign = TextAlign.Unspecified,
) {
    val state = rememberWheelPickerState(itemCount = items.size, initialIndex = initialIndex)
    val confirm = { onPicked.invoke(items[state.index]); onDismissRequest.invoke() }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            Box(Modifier.fillMaxWidth().wheelPickerKeys(state, confirm)) {
                WheelPicker(
                    modifier = Modifier.align(Alignment.Center),
                    state = state,
                    bufferSize = 6,
                    window = {
                        Box(
                            Modifier.background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                MaterialTheme.shapes.small,
                            )
                        )
                    }
                ) { index ->
                    Text(
                        text = itemToText.invoke(items[index]),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = textAlign,
                        modifier =
                            Modifier.padding(32.dp, 8.dp).graphicsLayer {
                                alpha = (BUFFER_SIZE - abs(state.value - index)).coerceIn(0f, 1f)
                            },
                        color =
                            lerp(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.onSurfaceVariant,
                                abs(state.value - index).coerceIn(0f, 1f),
                            ),
                    )
                }

            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(onClick = confirm) {
                Text("Select")
            }
        }
    )
}