package com.tambapps.pokemon.alakastats.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.theme.surfaceVariantDark
import com.tambapps.pokemon.alakastats.ui.theme.surfaceVariantLight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val LocalSnackBar = compositionLocalOf<SnackBar> {
    error("SnackBar not provided")
}

private val darkErrorColor = Color(0xFFA80000)
private val darkSuccessColor = Color(0xFF059600)

class SnackBar(
    private val state: SnackbarHostState,
    private val backgroundColorState: MutableState<Color>,
    private val isDarkTheme: Boolean
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
        backgroundColorState.value = backgroundColor(type)
        state.showSnackbar(message)
    }


    private fun backgroundColor(type: Severity) = if(isDarkTheme)
        when(type) {
            Severity.INFO -> surfaceVariantDark
            Severity.ERROR -> darkErrorColor
            Severity.SUCCESS -> darkSuccessColor
        }
    else when(type) {
        Severity.INFO -> surfaceVariantLight
        Severity.ERROR -> Color.Red
        Severity.SUCCESS -> Color.Green
    }
}

@Composable
fun SnackBarContext(
    content: @Composable () -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val backgroundColorState = remember { mutableStateOf(Color.Transparent) }
    val isDarkTheme = isSystemInDarkTheme()
    val snackBar = remember { SnackBar(snackBarHostState, backgroundColorState, isDarkTheme) }
    CompositionLocalProvider(LocalSnackBar provides snackBar) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            
            SnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .safeContentPadding()
                    .padding(bottom = 16.dp),
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        // apparently only containerColor works. I can't control the text color
                        containerColor = backgroundColorState.value,
                        )
                }
            )
        }
    }
}