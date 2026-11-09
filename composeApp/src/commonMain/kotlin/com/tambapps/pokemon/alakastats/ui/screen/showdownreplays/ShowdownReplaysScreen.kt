package com.tambapps.pokemon.alakastats.ui.screen.showdownreplays

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.delete
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
import androidx.compose.foundation.lazy.LazyColumn
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
    val viewModel = koinViewModel<ShowdownReplaysViewModel>()
    val isCompact = LocalIsCompact.current
    val shown = viewModel.shownReplays
    val grouped = viewModel.bestOf3 && viewModel.groupBySeries
    val listItems = remember(shown, viewModel.savedReplays, grouped) {
        if (grouped) groupedBySeries(shown, viewModel.savedReplays)
        else shown.map(ShowdownReplayListItem::Single)
    }
    val contentModifier = Modifier.widthIn(max = MAX_CONTENT_WIDTH).fillMaxWidth()
    LazyColumn(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(if (isCompact) Modifier.safeContentPadding() else Modifier.padding(all = 16.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = if (isCompact) 8.dp else 0.dp, vertical = 8.dp),
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
                item { ResultsHeader(viewModel, shown.size, contentModifier) }
                items(listItems, key = { it.key }) { item ->
                    when (item) {
                        is ShowdownReplayListItem.Single -> ShowdownReplayCard(item.replay, { viewModel.viewedReplay = item.replay }, contentModifier)
                        is ShowdownReplayListItem.Series -> ShowdownReplaySeriesCard(item, { viewModel.viewedReplay = it }, contentModifier)
                    }
                }
            }
        }
    }
    LoadDialog(viewModel)
    StorageDialog(viewModel)
    LoadConfirmationDialog(viewModel)
    DeletionConfirmationDialog(viewModel)
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
