package com.tambapps.pokemon.alakastats.ui

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
import com.tambapps.pokemon.alakastats.ui.theme.isDarkThemeEnabled
import com.tambapps.pokemon.alakastats.ui.theme.onSurfaceVariantDark
import com.tambapps.pokemon.alakastats.ui.theme.onSurfaceVariantLight
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
private val darkWarningColor = Color(0xFFB86000)
private val lightWarningColor = Color(0xFFFF9800)

/**
 * The colors of a snackbar. The text color must be set with the background: the default one (inverseOnSurface) is
 * meant for the default background (inverseSurface)
 */
data class SnackBarColors(val container: Color, val content: Color)

class SnackBar(
    private val state: SnackbarHostState,
    private val colorsState: MutableState<SnackBarColors>,
    private val isDarkTheme: Boolean
) {
    enum class Severity {
        INFO,
        WARNING,
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
        colorsState.value = colors(type)
        state.showSnackbar(message)
    }


    private fun colors(type: Severity) = if(isDarkTheme)
        when(type) {
            Severity.INFO -> SnackBarColors(surfaceVariantDark, onSurfaceVariantDark)
            Severity.WARNING -> SnackBarColors(darkWarningColor, Color.White)
            Severity.ERROR -> SnackBarColors(darkErrorColor, Color.White)
            Severity.SUCCESS -> SnackBarColors(darkSuccessColor, Color.White)
        }
    else when(type) {
        Severity.INFO -> SnackBarColors(surfaceVariantLight, onSurfaceVariantLight)
        Severity.WARNING -> SnackBarColors(lightWarningColor, Color.Black)
        Severity.ERROR -> SnackBarColors(Color.Red, Color.White)
        Severity.SUCCESS -> SnackBarColors(Color.Green, Color.Black)
    }
}

@Composable
fun SnackBarContext(
    content: @Composable () -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val colorsState = remember { mutableStateOf(SnackBarColors(Color.Transparent, Color.Unspecified)) }
    val isDarkTheme = isDarkThemeEnabled()
    val snackBar = remember { SnackBar(snackBarHostState, colorsState, isDarkTheme) }
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
                        containerColor = colorsState.value.container,
                        contentColor = colorsState.value.content,
                    )
                }
            )
        }
    }
}