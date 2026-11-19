package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.tambapps.pokemon.alakastats.ui.navigation.InterceptBack

/**
 * Leaving a form without saving: asks first if it has changes, for its own back or cancel buttons as for the system's
 * back and the browser's back button, which this takes over while in the composition
 *
 * @param hasChanges whether leaving would lose what the user entered
 * @param message what the dialog says, e.g. "This matchup has unsaved changes. Leave without saving them?"
 * @param onLeave leaves the form, discarding its changes
 * @return what the form's back and cancel buttons do
 */
@Composable
fun rememberDiscardChangesGuard(hasChanges: () -> Boolean, message: String, onLeave: () -> Unit): () -> Unit {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val currentHasChanges by rememberUpdatedState(hasChanges)
    val currentOnLeave by rememberUpdatedState(onLeave)
    val leave = remember { { if (currentHasChanges()) showDialog = true else currentOnLeave() } }
    InterceptBack(leave)
    if (showDialog) {
        DiscardChangesDialog(message, onDismiss = { showDialog = false }, onDiscard = { currentOnLeave() })
    }
    return leave
}

@Composable
private fun DiscardChangesDialog(message: String, onDismiss: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Discard Changes") },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onDiscard()
            }) {
                Text("Discard", color = Color.Red)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Editing")
            }
        }
    )
}
