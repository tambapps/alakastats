package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.replay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.alakastats.domain.model.OpenTeamSheet
import com.tambapps.pokemon.alakastats.domain.model.Player
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.composables.Pokepaste
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
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
    if (viewModel.replayToRemove != null) {
        RemoveReplayDialog(viewModel)
    }
}

@Composable
internal fun AddReplayButton(viewModel: TeamReplayViewModel) {
    OutlinedButton(onClick = {
        if (!viewModel.isLoading) {
            viewModel.showAddReplayDialog()
        }
    }) {
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
                    placeholder = { Text("https://replay.pokemonshowdown.com/...") },
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
            val snackBar = LocalSnackBar.current
            TextButton(
                onClick = { viewModel.addReplays(snackBar) },
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

@Composable
private fun RemoveReplayDialog(viewModel: TeamReplayViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.hideRemoveReplayDialog() },
        title = { Text("Remove replay") },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to remove this replay?",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.removeReplay() },
            ) {
                Text("Remove", color = Color.Red)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.hideRemoveReplayDialog() }) {
                Text("Cancel")
            }
        }
    )
}


@Composable
internal fun ViewReplayButton(url: String) {
    val uriHandler = LocalUriHandler.current
    OutlinedButton(
        onClick = { uriHandler.openUri(url) },
    ) {
        Text("Replay")
    }
}

@Composable
internal fun OtsButton(player: Player, ots: OpenTeamSheet, viewModel: TeamReplayViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = {showDialog = true },
    ) {
        Text("OTS")
    }
    if (!showDialog) {
        return
    }

    val pokepaste = remember { ots.toPokepaste() }
    AlertDialog(
        onDismissRequest = { showDialog = false },
        title = { Text("${player.name}'s team") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState())
            ) {
                Pokepaste(pokepaste, viewModel.pokemonImageService)
            }
        },
        confirmButton = {
            TextButton(onClick = { showDialog = false }) {
                Text("OK")
            }
        },
        dismissButton = {
            /* TODO copy to clipboard. unfortunately not that simple in KMP */
            val snackbar = LocalSnackBar.current
            TextButton(onClick = {
                showDialog = false
                snackbar.show("TODO: not implemented yet")
            }) {
                Text("Copy")
            }
        }
    )
}

@Composable
internal fun SelectedPokemon(pokemon: String, teraType: PokeType?, pokemonImageService: PokemonImageService, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val scale = 0.75f
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
            contentAlignment = Alignment.Center
        ) {
            pokemonImageService.PokemonArtwork(pokemon, modifier = Modifier.size(128.dp))
        }
        val offset = 16.dp
        teraType?.let {
            pokemonImageService.TeraTypeImage(it, modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 0.dp, y = -offset)
                .size(50.dp)
            )
        }
    }
}