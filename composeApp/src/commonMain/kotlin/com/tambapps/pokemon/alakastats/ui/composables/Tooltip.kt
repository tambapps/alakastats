package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlin.time.Duration.Companion.seconds

// Tooltip only works for Mobile
@Composable
fun TooltipIfEnabled(disabled: Boolean, tooltip: String, modifier: Modifier, composer: @Composable (Modifier) -> Unit) {
    if (disabled) {
        composer.invoke(modifier)
    } else {
        Tooltip(tooltip, modifier) {
            composer.invoke(Modifier)
        }
    }
}


@Composable
fun Tooltip(tooltip: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var show by remember { mutableStateOf(false) }

    LaunchedEffect(show) {
        if (show) {
            kotlinx.coroutines.delay(1.4.seconds)
            show = false
        }
    }

    Box(modifier) {
        Box(
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { show = true },
                )
            }
        ) {
            content()
        }
        if (show) {
            Popup(alignment = Alignment.BottomCenter) {
                val (background, text) =
                    if (isSystemInDarkTheme()) Pair(Color.Black, Color.White)
                    else Pair(Color.White, Color.Black)
                Box(
                    modifier = Modifier
                        .background(
                            color = background.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(tooltip, color = text)
                }
            }
        }
    }
}
