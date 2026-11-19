package com.tambapps.pokemon.alakastats.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

/**
 * The backs the screens take over, for the web app's browser back button: it changes the history, which the back
 * stack follows without going through the navigation's back handlers
 */
class BackInterceptors {
    private val handlers = mutableListOf<() -> Unit>()

    internal fun add(handler: () -> Unit) {
        handlers += handler
    }

    internal fun remove(handler: () -> Unit) {
        handlers -= handler
    }

    /** Gives the back to the screen that took it over last. @return whether one did */
    fun intercept(): Boolean {
        val handler = handlers.lastOrNull() ?: return false
        handler()
        return true
    }
}

val LocalBackInterceptors = staticCompositionLocalOf { BackInterceptors() }

/**
 * Takes over the back while in the composition, whether it's the system's (Android's gesture or button, the Escape
 * key) or the browser's back button: e.g. to ask before discarding changes, instead of leaving the screen
 */
@Composable
fun InterceptBack(onBack: () -> Unit) {
    NavigationBackHandler(rememberNavigationEventState(NavigationEventInfo.None), onBackCompleted = onBack)
    val interceptors = LocalBackInterceptors.current
    val currentOnBack by rememberUpdatedState(onBack)
    DisposableEffect(interceptors) {
        val handler = { currentOnBack() }
        interceptors.add(handler)
        onDispose { interceptors.remove(handler) }
    }
}
