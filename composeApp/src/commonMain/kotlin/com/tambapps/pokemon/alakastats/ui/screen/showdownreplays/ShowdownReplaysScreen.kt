package com.tambapps.pokemon.alakastats.ui.screen.showdownreplays

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.arrow_upward
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import alakastats.composeapp.generated.resources.delete
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import com.tambapps.pokemon.alakastats.ui.composables.LazyColumnWithScrollbar
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplaySummary
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.model.ShowdownReplayListItem
import com.tambapps.pokemon.alakastats.ui.model.groupedBySeries
import com.tambapps.pokemon.alakastats.ui.screen.replayviewer.HeaderIconButton
import com.tambapps.pokemon.alakastats.ui.screen.replayviewer.ReplayViewerDialog
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import org.koin.compose.viewmodel.koinViewModel

// the cards don't stretch on wide screens
private val MAX_CONTENT_WIDTH = 900.dp

/**
 * To download the public Showdown replays of a format and browse the saved ones
 */
@Composable
fun ShowdownReplaysScreen() {
    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        ShowdownReplaysList(listState)
        ScrollToTopButton(listState, Modifier.align(Alignment.BottomEnd))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShowdownReplaysList(listState: LazyListState) {
    val viewModel = koinViewModel<ShowdownReplaysViewModel>()
    val isCompact = LocalIsCompact.current
    val shown = viewModel.shownReplays
    val grouped = viewModel.bestOf3 && viewModel.groupBySeries
    val listItems = remember(shown, viewModel.savedReplays, grouped) {
        if (grouped) groupedBySeries(shown, viewModel.savedReplays)
        else shown.map(ShowdownReplayListItem::Single)
    }
    val contentModifier = Modifier.widthIn(max = MAX_CONTENT_WIDTH).fillMaxWidth()
    // on desktop, with a scrollbar on the right of the window: the margins are the list's, not around it
    LazyColumnWithScrollbar(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(if (isCompact) Modifier.safeContentPadding() else Modifier),
        state = listState,
        // room for the scroll to top button below the last replay
        contentPadding = if (isCompact) PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 80.dp)
        else PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Box(Modifier.fillMaxWidth()) {
                Text(
                    "Showdown Replays",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        item { ShowdownReplaysToolbar(viewModel, contentModifier) }
        when {
            viewModel.isLoadingSaved -> item { Text("Loading the saved replays…", modifier = contentModifier) }
            viewModel.savedReplays.isEmpty() -> item { NoSavedReplays(viewModel, contentModifier) }
            else -> {
                item { ShowdownReplayFiltersCard(viewModel, contentModifier) }
                // stays at the top while scrolling: the selection's actions are at hand
                stickyHeader(key = "results-header") {
                    Box(
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(vertical = 4.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        if (viewModel.isSelecting) SelectionBar(viewModel, contentModifier)
                        else ResultsHeader(viewModel, shown.size, contentModifier)
                    }
                }
                items(listItems, key = { it.key }) { item ->
                    val selecting = viewModel.isSelecting
                    val onReplayClick = { replay: ShowdownReplaySummary ->
                        if (selecting) viewModel.toggleSelection(replay) else viewModel.viewedReplay = replay
                    }
                    when (item) {
                        is ShowdownReplayListItem.Single -> ShowdownReplayCard(
                            replay = item.replay,
                            onClick = { onReplayClick(item.replay) },
                            modifier = contentModifier,
                            selected = if (selecting) item.replay.id in viewModel.selectedIds else null,
                        )
                        is ShowdownReplayListItem.Series -> ShowdownReplaySeriesCard(
                            series = item,
                            onGameClick = onReplayClick,
                            modifier = contentModifier,
                            selectedIds = if (selecting) viewModel.selectedIds else null,
                            onSeriesSelectionToggle = { viewModel.toggleSelection(item.games) },
                        )
                    }
                }
            }
        }
    }
    LoadDialog(viewModel)
    StorageDialog(viewModel)
    LoadConfirmationDialog(viewModel)
    DeletionConfirmationDialog(viewModel)
    TeamPickerDialog(viewModel)
    viewModel.viewedReplay?.let { replay -> ShowdownReplayViewer(viewModel, replay) }
    ReplayDeletionConfirmationDialog(viewModel)
}

/** How many replays the filters keep, and the grouping of the best-of-3s */
@Composable
private fun ResultsHeader(viewModel: ShowdownReplaysViewModel, shownCount: Int, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        val savedCount = viewModel.savedReplays.size
        Text(
            if (shownCount == savedCount) "$savedCount replays" else "$shownCount of $savedCount replays",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (viewModel.bestOf3) {
            FilterChip(
                selected = viewModel.groupBySeries,
                onClick = { viewModel.groupBySeries = !viewModel.groupBySeries },
                label = { Text("Group by Bo3", maxLines = 1) },
            )
        }
        TextButton(onClick = viewModel::startSelection, enabled = shownCount > 0) { Text("Select") }
    }
}

/**
 * Plays a saved replay back, with the actions of a saved replay: copying its link, opening it on Showdown, deleting it
 */
@Composable
private fun ShowdownReplayViewer(viewModel: ShowdownReplaysViewModel, replay: ShowdownReplaySummary) {
    val close = { viewModel.viewedReplay = null }
    ReplayViewerDialog(
        replayUrl = replay.url,
        browserUrl = replay.url,
        textToCopy = replay.url,
        asP2 = false,
        onDismissRequest = close,
        title = replay.players.joinToString(" vs ") { it.name.value },
        actions = {
            HeaderIconButton(Res.drawable.delete, "Delete from this device") {
                // closed for the confirmation to show: a dialog opened from another one would be behind it
                close()
                viewModel.replayToDelete = replay
            }
        },
    )
}

@Composable
private fun ReplayDeletionConfirmationDialog(viewModel: ShowdownReplaysViewModel) {
    val replay = viewModel.replayToDelete ?: return
    val snackBar = LocalSnackBar.current
    val dismiss = { viewModel.replayToDelete = null }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Delete replay") },
        text = {
            Text(
                "The replay ${replay.players.joinToString(" vs ") { it.name.value }} will be deleted from this device. " +
                    "It can be downloaded again."
            )
        },
        confirmButton = {
            TextButton(onClick = {
                dismiss()
                viewModel.deleteReplay(replay)
                snackBar.show("Replay deleted")
            }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } },
    )
}

/**
 * Shown once the top of the list is scrolled out of sight, to go back to its toolbar and filters
 */
@Composable
private fun ScrollToTopButton(listState: LazyListState, modifier: Modifier = Modifier) {
    val isCompact = LocalIsCompact.current
    val scope = rememberCoroutineScope()
    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    AnimatedVisibility(
        visible = scrolled,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
            // above the system's navigation bar on a phone, beside the scrollbar on desktop
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.End))
            .padding(end = if (isCompact) 16.dp else 32.dp, bottom = if (isCompact) 16.dp else 24.dp),
    ) {
        SmallFloatingActionButton(onClick = { scope.launch { listState.animateScrollToItem(0) } }) {
            Icon(painterResource(Res.drawable.arrow_upward), contentDescription = "Scroll to the top")
        }
    }
}
