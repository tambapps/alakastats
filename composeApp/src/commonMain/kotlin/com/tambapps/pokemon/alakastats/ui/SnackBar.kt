package com.tambapps.pokemon.alakastats.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val LocalSnackBar = compositionLocalOf<SnackBar> {
    error("SnackBar not provided")
}

class SnackBar(
    private val state: SnackbarHostState,
    private val backgroundColorState: MutableState<Color>
) {
    enum class Severity {
        INFO,
        ERROR,
        SUCCESS
    }
    private val scope = CoroutineScope(Dispatchers.Main)

    // not thread safe because it changes color
    fun show(message: String, type: Severity = Severity.INFO) {
        scope.launch {
            showNow(message, type)
        }
    }

    // not thread safe because it changes color
    suspend fun showNow(message: String, type: Severity = Severity.INFO) {
        backgroundColorState.value = when(type) {
            Severity.INFO -> Color.White
            Severity.ERROR -> Color.Red
            Severity.SUCCESS -> Color.Green
        }
        state.showSnackbar(message)
    }
}

@Composable
fun SnackBarContext(
    content: @Composable () -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val backgroundColorState = remember { mutableStateOf(Color.Transparent) }
    val snackBar = remember { SnackBar(snackBarHostState, backgroundColorState) }
    CompositionLocalProvider(LocalSnackBar provides snackBar) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(
                    hostState = snackBarHostState,
                    snackbar = { data ->
                        Snackbar(
                            snackbarData = data,
                            // apparently only containerColor works. I can't control the text color
                            containerColor = backgroundColorState.value,
                            )
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                content()
            }
        }
    }
}