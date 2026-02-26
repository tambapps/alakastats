package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.swmansion.kmpwheelpicker.WheelPicker
import com.swmansion.kmpwheelpicker.rememberWheelPickerState
import kotlin.math.abs


private const val BUFFER_SIZE = 3

@Composable
fun <T> WheelPickerDialog(
    title: String,
    items: List<T>,
    onPicked: (T) -> Unit,
    onDismissRequest: () -> Unit,
    initialIndex: Int = 0,
    itemToText: (T) -> String = { it.toString() },
) {
    val state = rememberWheelPickerState(itemCount = items.size, initialIndex = initialIndex)
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            Box(Modifier.fillMaxWidth()) {
                WheelPicker(
                    modifier = Modifier.align(Alignment.Center),
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
                    Text(
                        text = itemToText.invoke(items[index]),
                        style = MaterialTheme.typography.titleMedium,
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
            TextButton(onClick = { onPicked.invoke(items[state.index]); onDismissRequest.invoke() }) {
                Text("Select")
            }
        }
    )
}