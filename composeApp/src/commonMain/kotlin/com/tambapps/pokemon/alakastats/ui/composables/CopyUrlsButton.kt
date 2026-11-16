package com.tambapps.pokemon.alakastats.ui.composables

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.content_copy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboard
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.SnackBar
import com.tambapps.pokemon.alakastats.util.copyToClipboard
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/**
 * Copies the replays' URLs to the clipboard, one per line
 *
 * @param urls computed on click, e.g. the selected replays' in the order they're shown
 */
@Composable
fun CopyReplayUrlsButton(urls: () -> List<String>, enabled: Boolean = true) {
    val snackbar = LocalSnackBar.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    IconButton(
        enabled = enabled,
        onClick = {
            val copied = urls()
            if (copied.isEmpty()) return@IconButton
            scope.launch {
                if (copyToClipboard(clipboard, label = "Replays", text = copied.joinToString("\n"))) {
                    snackbar.show(if (copied.size == 1) "1 URL copied" else "${copied.size} URLs copied")
                } else {
                    snackbar.show("Copy to clipboard not supported", SnackBar.Severity.ERROR)
                }
            }
        },
    ) {
        Icon(painterResource(Res.drawable.content_copy), contentDescription = "Copy the selected replays' URLs")
    }
}
