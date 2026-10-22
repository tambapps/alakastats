package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import com.tambapps.pokemon.alakastats.PlatformType
import com.tambapps.pokemon.alakastats.platform

/**
 * On iOS, a swipe from the left edge of the screen goes back. Compose turns that swipe into a back event, which the
 * navigator doesn't handle on iOS. Does nothing elsewhere (on Android, the navigator handles the system back).
 */
// BackHandler is deprecated for NavigationEventHandler, whose multiplatform Compose artifact isn't a dependency yet
@Suppress("DEPRECATION")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun IosSwipeBack(onBack: () -> Unit) {
    if (platform.type != PlatformType.Ios) return
    BackHandler(onBack = onBack)
}
