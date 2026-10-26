package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Two-way channel between the app and the page of an [HtmlView]
 * @param onMessage receives what the page sends with postToApp(message), defined by [HTML_BRIDGE_JS]
 */
class HtmlBridge(internal val onMessage: (String) -> Unit) {
    // set by the platform's view once it exists
    internal var evaluator: ((String) -> Unit)? = null

    /**
     * Runs a script in the page. Does nothing while the view isn't there yet
     */
    fun evaluate(script: String) {
        evaluator?.invoke(script)
    }
}

// the names each platform exposes its side of the bridge under
internal const val ANDROID_BRIDGE_NAME = "AlakastatsAndroid"
internal const val IOS_BRIDGE_NAME = "alakastats"

/**
 * Defines postToApp(message) for a page shown in an [HtmlView], whichever the platform
 */
const val HTML_BRIDGE_JS = """
  function postToApp(message) {
    if (window.$ANDROID_BRIDGE_NAME) {
      window.$ANDROID_BRIDGE_NAME.postMessage(message);
    } else if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.$IOS_BRIDGE_NAME) {
      window.webkit.messageHandlers.$IOS_BRIDGE_NAME.postMessage(message);
    } else if (window.parent !== window) {
      window.parent.postMessage(message, '*');
    }
  }
"""

/**
 * Shows an HTML page: in a native web view on mobile, in an iframe over the canvas on the web
 * @param html the page
 * @param baseUrl the URL the page's relative links resolve against, where the platform lets us set it
 * @param bridge to talk with the page
 * @param obscured whether a menu or a dialog is over the view. The web's iframe would hide it, so it hides itself
 */
@Composable
expect fun HtmlView(
    html: String,
    baseUrl: String,
    bridge: HtmlBridge,
    modifier: Modifier = Modifier,
    obscured: Boolean = false,
)
