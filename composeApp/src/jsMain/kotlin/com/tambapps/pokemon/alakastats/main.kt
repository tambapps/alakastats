package com.tambapps.pokemon.alakastats

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import kotlinx.browser.document
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    window.addEventListener("load", {
        CanvasBasedWindow(canvasElementId = "ComposeTarget") {
            App()
        }
    })
}