package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.replay

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact

@Composable
fun TeamReplayTab(viewModel: TeamReplayViewModel) {
    val isCompact = LocalIsCompact.current
    if (isCompact) {
        TeamReplayTabMobile(viewModel)
    } else {
        TeamReplayTabDesktop(viewModel)
    }
    
    if (viewModel.showAddReplayDialog) {
        AddReplayDialog(viewModel)
    }
}

@Composable
internal fun AddReplayButton(viewModel: TeamReplayViewModel) {
    OutlinedButton(onClick = { viewModel.showAddReplayDialog() }) {
        Text("Add Replay")
    }
}

@Composable
private fun AddReplayDialog(viewModel: TeamReplayViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.hideAddReplayDialog() },
        title = { Text("Add Replays") },
        text = {
            Column {
                Text(
                    text = "Enter replay URLs separated by comma (,) or spaces:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = viewModel.replayUrlsText,
                    onValueChange = { viewModel.updateReplayUrlsText(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://replay.pokemonshowdown.com/... https://replay.pokemonshowdown.com/...") },
                )
                
                viewModel.getValidationMessage()?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (message.contains("No valid")) Color.Red else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.addReplays() },
                enabled = viewModel.replayUrlsText.isNotBlank() && viewModel.getValidationMessage()?.contains("No valid") != true
            ) {
                Text("Add Replays")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.hideAddReplayDialog() }) {
                Text("Cancel")
            }
        }
    )
}

