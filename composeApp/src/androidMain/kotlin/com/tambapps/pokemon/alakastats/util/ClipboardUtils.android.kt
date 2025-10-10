package com.tambapps.pokemon.alakastats.util

import android.content.ClipData
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.toClipEntry

actual suspend fun copyToClipboard(clipboard: Clipboard, label: String, text: String): Boolean {
    clipboard.setClipEntry(ClipData.newPlainText(label, text).toClipEntry())
    return true
}
