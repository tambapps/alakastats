package com.tambapps.pokemon.alakastats.util

import androidx.compose.ui.platform.Clipboard

expect suspend fun copyToClipboard(clipboard: Clipboard, label: String, text: String): Boolean
