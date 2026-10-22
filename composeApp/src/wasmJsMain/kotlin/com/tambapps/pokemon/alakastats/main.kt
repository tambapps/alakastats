package com.tambapps.pokemon.alakastats

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Compose gets its own container rather than the body: it attaches a shadow root to it, which hides any element
    // added afterwards to the same parent, as Google's file picker dialog
    val container = (document.createElement("div") as HTMLElement).apply {
        id = "compose-root"
        style.width = "100%"
        style.height = "100%"
    }
    document.body!!.appendChild(container)
    ComposeViewport(container) {
        WasmApp()
    }
}
