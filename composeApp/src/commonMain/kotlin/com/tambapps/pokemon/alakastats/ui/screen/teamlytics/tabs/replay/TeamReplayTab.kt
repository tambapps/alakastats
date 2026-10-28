package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.replay

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.add
import alakastats.composeapp.generated.resources.arrow_forward
import alakastats.composeapp.generated.resources.mega
import alakastats.composeapp.generated.resources.open_in_new
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.TeraType
import com.tambapps.pokemon.alakastats.domain.model.BestOf
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.domain.model.GameOutcome
import com.tambapps.pokemon.alakastats.domain.model.OpenTeamSheet
import com.tambapps.pokemon.alakastats.domain.model.Player
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.isShowdownReplayUrl
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.composables.FabLayout
import com.tambapps.pokemon.alakastats.ui.composables.LOOSE_COLOR
import com.tambapps.pokemon.alakastats.ui.composables.ScrollToTopIfNeeded
import com.tambapps.pokemon.alakastats.ui.composables.Tooltip
import com.tambapps.pokemon.alakastats.ui.composables.VerticalPokepaste
import com.tambapps.pokemon.alakastats.ui.composables.WIN_COLOR
import com.tambapps.pokemon.alakastats.ui.screen.editteam.EditTeamScreen
import com.tambapps.pokemon.alakastats.ui.screen.manualreplay.ManualReplayScreen
import com.tambapps.pokemon.alakastats.ui.screen.replayviewer.ReplayViewerDialog
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.FiltersBar
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.rememberGuardedChange
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.TeamlyticsFiltersTabViewModel
import com.tambapps.pokemon.alakastats.ui.service.FacingDirection
import com.tambapps.pokemon.alakastats.ui.service.PokemonArtwork
import com.tambapps.pokemon.alakastats.ui.service.TeraTypeImage
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.util.copyToClipboard
import com.tambapps.pokemon.alakastats.util.titlecase
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
fun TeamReplayTab(viewModel: TeamReplayViewModel) {
    val isCompact = LocalIsCompact.current
    FabLayout(
        fab = {
            AddReplayButton(viewModel)
        }
    ) {
        val scrollState = rememberLazyListState()
        if (viewModel.hasNoReplaysToShow) {
            NoReplay(viewModel)
        } else if (isCompact) {
            TeamReplayTabMobile(viewModel, scrollState)
        } else {
            TeamReplayTabDesktop(viewModel, scrollState)
        }
        ScrollToTopIfNeeded(viewModel, scrollState)

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
    // a viewer of a shared team can't add replays
    if (viewModel.isReadOnly) return
    val guardedChange = rememberGuardedChange()
    FloatingActionButton(
        onClick = {
            if (!viewModel.isLoading) {
                guardedChange { viewModel.showAddReplayDialog() }
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
private fun AddReplayDialog(viewModel: TeamReplayViewModel) {
    val navigator = LocalNavigator.currentOrThrow
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
                onClick = {
                    viewModel.hideAddReplayDialog()
                    navigator.push(
                        ManualReplayScreen(viewModel.team, viewModel.formatData) { replay ->
                            viewModel.saveManualReplay(snackBar, replay)
                        }
                    )
                },
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text("Enter Manually")
            }

            TextButton(
                onClick = { viewModel.addReplays(snackBar) },
                enabled = viewModel.replayUrlsText.isNotBlank() && viewModel.getValidationMessage()
                    ?.contains("No valid") != true
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

private val playerNameTextStyle @Composable get() = MaterialTheme.typography.titleLarge

@Composable
internal fun ColumnScope.PlayerNameEloText(player: Player, playerName: String) {
    Text(playerName, style = playerNameTextStyle)
    Spacer(Modifier.height(8.dp))

    if (player.beforeElo != null && player.afterElo != null) {
        val iconSize = 28.dp
        val won = player.beforeElo < player.afterElo
        val outputColor = if (won) WIN_COLOR else LOOSE_COLOR
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(Res.drawable.arrow_forward),
                contentDescription = "UP/DOWN",
                modifier = Modifier.rotate(if (won) -90f else 90f).size(iconSize),
                tint = outputColor
            )
            Spacer(Modifier.width(4.dp))
            SelectionContainer {
                Text(player.beforeElo.toString(), style = playerNameTextStyle)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(iconSize + 4.dp))
            SelectionContainer {
                Text(player.afterElo.toString(), style = playerNameTextStyle, color = outputColor)
            }
        }
    } else if (player.beforeElo != null) {
        SelectionContainer {
            Text(player.beforeElo.toString(), style = playerNameTextStyle)
        }
    }
}

@Composable
internal fun ViewReplayButton(
    team: Teamlytics,
    replay: ReplayAnalytics,
    url: String,
    modifier: Modifier = Modifier
) {
    if (!isShowdownReplayUrl(url)) {
        // e.g. a YouTube video: opened in a new tab
        val uriHandler = LocalUriHandler.current
        OutlinedButton(modifier = modifier, onClick = { uriHandler.openUri(url) }) {
            Text("Replay")
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(Res.drawable.open_in_new),
                contentDescription = "Opens in a new tab",
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
        }
        return
    }
    val asP2 = !team.sdNames.contains(replay.player1.name)
    var browserUrl = url.removeSuffix(".json")
    if (asP2) {
        browserUrl += "?p2"
    }
    var showViewer by remember { mutableStateOf(false) }

    OutlinedButton(
        modifier = modifier,
        onClick = { showViewer = true },
    ) {
        Text("Replay")
    }
    if (showViewer) {
        ReplayViewerDialog(url, browserUrl, asP2, onDismissRequest = { showViewer = false })
    }
}

@Composable
internal fun EditSdNamesButton(viewModel: TeamReplayViewModel) {
    val navigator = LocalNavigator.currentOrThrow
    val guardedChange = rememberGuardedChange()

    // don't push because when coming to the screen, this screen doesn't recompose and take into account the updated team
    OutlinedButton(onClick = {
        guardedChange {
            navigator.replace(
                EditTeamScreen(
                    viewModel.team.id,
                    redirectToTeamlyticsScreen = true
                )
            )
        }
    }) {
        Text("Edit Showdown Names")
    }
}

@Composable
internal fun OtsButton(
    format: Format,
    player: Player,
    ots: OpenTeamSheet,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    OutlinedButton(
        modifier = modifier,
        onClick = { showDialog = true },
    ) {
        Text("OTS")
    }
    if (!showDialog) {
        return
    }

    val pokepaste = ots.toPokepaste()
    AlertDialog(
        onDismissRequest = { showDialog = false },
        title = { Text("${player.name.value}'s team") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
            ) {
                // Mobile on purpose because we want a vertical pokepaste display on desktop too, as
                // for some mysterious reason the dialog can't have full screen width
                VerticalPokepaste(pokepaste, format)
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
            val scope = rememberCoroutineScope()
            TextButton(onClick = {
                showDialog = false
                scope.launch {
                    if (copyToClipboard(
                            clipboardManager,
                            label = "${player.name.value}'s OTS",
                            text = ots.toPokepaste().toPokePasteString()
                        )
                    ) {
                        snackbar.show("Copied to clipboard")
                    } else {
                        snackbar.show("Copy to clipboard not supported")
                    }
                }
            }) {
                Text("COPY")
            }
        }
    )
}

@Composable
internal fun SelectedPokemon(
    pokemon: PokemonName,
    megaPokemon: PokemonName?,
    teraType: TeraType?,
    isYouPlayer: Boolean,
    modifier: Modifier = Modifier
) {
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
            PokemonArtwork(
                megaPokemon ?: pokemon,
                modifier = Modifier.height(128.dp),
                facingDirection = if (isYouPlayer) FacingDirection.RIGHT else FacingDirection.LEFT
            )
        }
        val offset = 16.dp
        megaPokemon?.let {
            Image(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 0.dp, y = -offset)
                    .size(50.dp),
                painter = painterResource(Res.drawable.mega),
                contentDescription = "Mega",
            )
        }

        teraType?.let { teraType ->
            Tooltip(teraType.name.titlecase(), modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 0.dp, y = -offset)
            ) {
                TeraTypeImage(
                    teraType, modifier = Modifier.size(50.dp)
                )
            }
        }
    }
}

@Composable
internal fun ReplayDropDownMenu(
    isMenuExpandedState: MutableState<Boolean>,
    viewModel: TeamReplayViewModel,
    replay: ReplayAnalytics
) {
    DropdownMenu(
        expanded = isMenuExpandedState.value,
        onDismissRequest = { isMenuExpandedState.value = false }
    ) {
        val snackBar = LocalSnackBar.current
        val alreadyHasNotes = !replay.notes.isNullOrBlank()
        // a viewer of a shared team can't change the replays
        if (viewModel.isReadOnly) {
            DropdownMenuItem(text = { Text("Shared with you as a viewer") }, onClick = { isMenuExpandedState.value = false }, enabled = false)
            return@DropdownMenu
        }

        val guardedChange = rememberGuardedChange()
        // only manual replays can be edited, the others are what showdown says they are
        if (replay.isManual) {
            val navigator = LocalNavigator.currentOrThrow
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {
                    isMenuExpandedState.value = false
                    guardedChange {
                        navigator.push(
                            ManualReplayScreen(viewModel.team, viewModel.formatData, replayToEdit = replay) { editedReplay ->
                                viewModel.saveManualReplay(snackBar, editedReplay, replacing = replay)
                            }
                        )
                    }
                }
            )
        }

        // a manual replay's notes are edited with the rest of it
        if (!replay.isManual) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (!alreadyHasNotes) "Add notes" else "Edit notes"
                    )
                },
                onClick = {
                    guardedChange { viewModel.showNoteReplayDialog(replay) }
                    isMenuExpandedState.value = false
                }
            )

            if (alreadyHasNotes) {
                DropdownMenuItem(
                    text = { Text("Remove notes") },
                    onClick = {
                        guardedChange { viewModel.editNotes(replay, null) }
                        isMenuExpandedState.value = false
                    }
                )
            }
        }

        // another link (e.g. a YouTube video) has nothing to fetch
        replay.showdownUrl?.let { replayUrl ->
            DropdownMenuItem(
                text = { Text("Reload") },
                onClick = {
                    guardedChange { viewModel.reloadReplay(snackBar, replay, replayUrl) }
                    isMenuExpandedState.value = false
                }
            )
        }

        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = {
                guardedChange { viewModel.showRemoveReplayDialog(replay) }
                isMenuExpandedState.value = false
            }
        )
    }
}

@Composable
internal fun VsText(
    currentPlayer: Player,
    opponentPlayer: Player,
    gameOutcome: GameOutcome,
    isInternalMatch: Boolean,
    bestOf: BestOf?,
    modifier: Modifier = Modifier,
    // in its series' card, which already tells the opponent: the game's number is enough
    inSeries: Boolean = false,
) {
    val gameTitle = bestOf?.takeIf { inSeries }?.let { "Game ${it.game}" }
    val text = when {
        gameTitle != null -> gameTitle
        gameOutcome == GameOutcome.UNKNOWN -> "${currentPlayer.name.value}\nVS\n${opponentPlayer.name.value}"
        // both players are the user's: the outcome is the first one's
        isInternalMatch -> "${currentPlayer.name.value} VS ${opponentPlayer.name.value}"
        else -> "VS ${opponentPlayer.name.value}"
    }
    Column(modifier = modifier.padding(start = 8.dp)) {
        SelectionContainer {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = if (gameTitle != null) FontWeight.Bold else null,
                textAlign = if (gameOutcome != GameOutcome.UNKNOWN || gameTitle != null) null else TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }
        bestOf?.takeIf { gameTitle == null }?.let {
            Text(
                text = "Game ${it.game} of ${it.of}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (isInternalMatch) {
            Text(
                text = "Internal match, not counted in the stats",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun NoReplay(viewModel: TeamlyticsFiltersTabViewModel) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(if (!viewModel.useCase.hasFilteredReplays) "No replays were found" else "No replays matched the filters")
        }

        FiltersBar(viewModel, Modifier.align(Alignment.TopStart).padding(16.dp))
    }
}
