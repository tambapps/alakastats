package com.tambapps.pokemon.alakastats.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Signal<T>(
    initialValue: T
) {
    var value by mutableStateOf(initialValue)
        private set

    fun emit(value: T) {
        this@Signal.value = value
    }

    @Composable
    fun Listen(callback: suspend (T) -> Unit) {
        LaunchedEffect(value) {
            callback.invoke(value)
        }
    }
}
class VoidSignal {

    private val signal = Signal(0)

    fun emit() = signal.emit(signal.value + 1)

    @Composable
    fun Listen(callback: suspend () -> Unit) = signal.Listen { callback.invoke() }
}