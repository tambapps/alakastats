package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.events.Event

/**
 * Compose draws everything on a canvas, which can't hold HTML: the page goes in an iframe laid over the canvas,
 * where this composable is. Anything Compose would draw on top of it (a dialog, a menu) ends up below it
 */
@Composable
actual fun HtmlView(html: String, baseUrl: String, bridge: HtmlBridge, modifier: Modifier, obscured: Boolean) {
    val density = LocalDensity.current
    val iframe = remember {
        (document.createElement("iframe") as HTMLIFrameElement).apply {
            style.position = "fixed"
            style.border = "none"
            style.zIndex = "1"
            // hidden until positioned
            style.width = "0px"
            style.height = "0px"
        }
    }
    DisposableEffect(iframe, bridge) {
        document.body!!.appendChild(iframe)
        // the page posts to its parent window, which also gets the messages of others, as Google's file picker
        val listener: (Event) -> Unit = { event ->
            messageFrom(event, iframe)?.let { bridge.onMessage(it.toString()) }
        }
        window.addEventListener("message", listener)
        bridge.evaluator = { script -> evaluateIn(iframe, script) }
        onDispose {
            bridge.evaluator = null
            window.removeEventListener("message", listener)
            iframe.remove()
        }
    }
    // srcdoc pages have no URL of their own, so baseUrl doesn't apply: the embed script only uses absolute URLs
    LaunchedEffect(html) {
        iframe.srcdoc = html
    }
    LaunchedEffect(obscured) {
        iframe.style.visibility = if (obscured) "hidden" else "visible"
    }
    Box(
        modifier.onGloballyPositioned { coordinates ->
            val bounds = coordinates.boundsInWindow()
            // bounds are relative to Compose's container, in device pixels
            val container = document.getElementById("compose-root")?.getBoundingClientRect()
            with(density) {
                iframe.style.left = "${(container?.left ?: 0.0) + bounds.left.toDp().value}px"
                iframe.style.top = "${(container?.top ?: 0.0) + bounds.top.toDp().value}px"
                iframe.style.width = "${bounds.width.toDp().value}px"
                iframe.style.height = "${bounds.height.toDp().value}px"
            }
        }
    )
}

/**
 * The message, if the event is a string message from this iframe's page
 */
private fun messageFrom(event: Event, iframe: HTMLIFrameElement): JsString? =
    js("event.source === iframe.contentWindow && typeof event.data === 'string' ? event.data : null")

// srcdoc pages share the app's origin, so the app can reach into them
private fun evaluateIn(iframe: HTMLIFrameElement, script: String): Unit =
    js("{ if (iframe.contentWindow) iframe.contentWindow.eval(script); }")
