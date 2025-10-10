package com.tambapps.pokemon.alakastats.util

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun copyToClipboard(clipboard: Clipboard, label: String, text: String): Boolean {
    clipboard.setClipEntry(ClipEntry.withPlainText(text))
    return true
}