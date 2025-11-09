package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.replay

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.add
import alakastats.composeapp.generated.resources.tune
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.TeraType
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.OpenTeamSheet
import com.tambapps.pokemon.alakastats.domain.model.Player
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.usecase.ConsultTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.composables.VerticalPokepaste
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.NbReplaysText
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.WinRateText
import com.tambapps.pokemon.alakastats.ui.service.FacingDirection
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import org.jetbrains.compose.resources.painterResource

@Composable
fun TeamReplayTab(viewModel: TeamReplayViewModel) {
    val isCompact = LocalIsCompact.current
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isCompact) {
            TeamReplayTabMobile(viewModel)
        } else {
            TeamReplayTabDesktop(viewModel)
        }


        val (padX, padY) = if (LocalIsCompact.current) 32.dp to 32.dp else 50.dp to 50.dp

        Row(modifier = Modifier.align(Alignment.BottomEnd).padding(end = padX, bottom = padY)) {
            FiltersButton(viewModel)
            Spacer(Modifier.width(32.dp))
            AddReplayButton(viewModel)
        }

        if (viewModel.showAddReplayDialog) {
            AddReplayDialog(viewModel)
        }
        viewModel.replayToNote?.let {
            NotesDialog(viewModel, it)
        }
        if (viewModel.replayToRemove != null) {
            RemoveReplayDialog(viewModel)
        }
    }
}

@Composable
private fun AddReplayButton(viewModel: TeamReplayViewModel, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = {
            if (!viewModel.isLoading) {
                viewModel.showAddReplayDialog()
            }
        },
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(Res.drawable.add),
            contentDescription = "Add",
        )
    }
}

@Composable
internal fun FiltersButton(viewModel: TeamReplayViewModel, modifier: Modifier = Modifier) {
    BadgedBox(
        modifier = modifier,
        badge = {
            if (viewModel.filters.hasAny()) {
                Badge(Modifier.size(16.dp))
            }
        }
    ) {
        FloatingActionButton(
            onClick = { viewModel.openFilters() },
        ) {
            Icon(
                painter = painterResource(Res.drawable.tune),
                contentDescription = "Filters",
            )
        }
    }
}

@Composable
private fun AddReplayDialog(viewModel: TeamReplayViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.hideAddReplayDialog() },
        title = { Text("Add Replay(s)") },
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
                Text("Add Replay(s)")
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
private fun NotesDialog(viewModel: TeamReplayViewModel, replay: ReplayAnalytics) {
    val alreadyHasNotes = !replay.notes.isNullOrBlank()

    AlertDialog(
        onDismissRequest = { viewModel.hideNoteReplayDialog() },
        title = { Text("Note Replay") },
        text = {
            Column {
                OutlinedTextField(
                    value = viewModel.replayNotesText,
                    onValueChange = { viewModel.replayNotesText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Notes") },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.editNotes(viewModel.replayNotesText) },
            ) {
                Text(if (alreadyHasNotes) "Edit Notes" else "Add Notes")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.hideNoteReplayDialog() }) {
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
internal fun ViewReplayButton(team: Teamlytics, replay: ReplayAnalytics, url: String, modifier: Modifier = Modifier) {
    var url = url.removeSuffix(".json")
    if (!team.sdNames.contains(replay.player1.name)) {
        url += "?p2"
    }
    val uriHandler = LocalUriHandler.current

    OutlinedButton(
        modifier= modifier,
        onClick = { uriHandler.openUri(url) },
    ) {
        Text("Replay")
    }
}

@Composable
internal fun OtsButton(player: Player, ots: OpenTeamSheet, viewModel: TeamReplayViewModel, modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }
    OutlinedButton(
        modifier = modifier,
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
                Modifier.verticalScroll(rememberScrollState()),
            ) {
                // Mobile on purpose because we want a vertical pokepaste display on desktop too, as
                // for some mysterious reason the dialog can't have full screen width
                VerticalPokepaste(pokepaste, viewModel.pokemonImageService,)
            }
        },
        confirmButton = {
            TextButton(onClick = { showDialog = false }) {
                Text("OK")
            }
        },
        dismissButton = {
            val snackbar = LocalSnackBar.current
            val clipboardManager = LocalClipboard.current
            TextButton(onClick = {
                showDialog = false
                viewModel.copyToClipboard(clipboardManager, snackbar, "${player.name}'s OTS", ots.toPokepaste().toPokePasteString())
            }) {
                Text("COPY")
            }
        }
    )
}

@Composable
internal fun SelectedPokemon(pokemon: PokemonName, teraType: TeraType?, pokemonImageService: PokemonImageService,
                             isYouPlayer: Boolean,
                             modifier: Modifier = Modifier) {
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
            pokemonImageService.PokemonArtwork(pokemon, modifier = Modifier.height(128.dp), facingDirection = if (isYouPlayer) FacingDirection.RIGHT else FacingDirection.LEFT)
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

@Composable
internal fun ReplayDropDownMenu(isMenuExpandedState: MutableState<Boolean>, viewModel: TeamReplayViewModel, replay: ReplayAnalytics) {
    DropdownMenu(
        expanded = isMenuExpandedState.value,
        onDismissRequest = { isMenuExpandedState.value = false }
    ) {
        val snackBar = LocalSnackBar.current
        val alreadyHasNotes = !replay.notes.isNullOrBlank()
        DropdownMenuItem(
            text = { Text(
                if (!alreadyHasNotes) "Add notes" else "Edit notes"
            ) },
            onClick = {
                viewModel.showNoteReplayDialog(replay)
                isMenuExpandedState.value = false
            }
        )

        if (alreadyHasNotes) {
            DropdownMenuItem(
                text = { Text("Remove notes") },
                onClick = {
                    viewModel.editNotes(replay, null)
                    isMenuExpandedState.value = false
                }
            )
        }

        replay.url?.let { replayUrl ->
            DropdownMenuItem(
                text = { Text("Reload") },
                onClick = {
                    viewModel.reloadReplay(snackBar, replay, replayUrl)
                    isMenuExpandedState.value = false
                }
            )
        }

        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = {
                viewModel.showRemoveReplayDialog(replay)
                isMenuExpandedState.value = false
            }
        )
    }
}

@Composable
internal fun Header(useCase: ConsultTeamlyticsUseCase) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.weight(1f))
        NbReplaysText(useCase)
        Spacer(Modifier.width(32.dp))
        WinRateText(useCase)
        Spacer(Modifier.weight(1f))
    }
    Spacer(Modifier.height(32.dp))
}
