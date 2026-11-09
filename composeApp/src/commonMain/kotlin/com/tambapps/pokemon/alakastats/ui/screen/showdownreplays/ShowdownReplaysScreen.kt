package com.tambapps.pokemon.alakastats.ui.screen.showdownreplays

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplayPlayer
import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplaySummary
import com.tambapps.pokemon.alakastats.ui.composables.FormatSelector
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.PokemonTeamPreview
import com.tambapps.pokemon.alakastats.ui.model.formatReplayDate
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
        item { LoadReplaysCard(viewModel, Modifier.widthIn(max = MAX_CONTENT_WIDTH).fillMaxWidth()) }
        item {
            Text(
                when {
                    viewModel.isLoadingSaved -> "Loading the saved replays…"
                    viewModel.savedReplays.isEmpty() -> "No replay saved for this format yet"
                    else -> "${viewModel.savedReplays.size} saved replays"
                },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.widthIn(max = MAX_CONTENT_WIDTH).fillMaxWidth(),
            )
        }
        items(viewModel.savedReplays, key = { it.id }) { replay ->
            ShowdownReplayCard(replay, Modifier.widthIn(max = MAX_CONTENT_WIDTH).fillMaxWidth())
        }
    }
    LoadConfirmationDialog(viewModel)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LoadReplaysCard(viewModel: ShowdownReplaysViewModel, modifier: Modifier = Modifier) {
    val enabled = !viewModel.isBusy
    MyCard(modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Load public replays", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                FormatSelector(
                    format = viewModel.format,
                    onFormatChange = viewModel::selectFormat,
                    formats = viewModel.formats,
                    enabled = enabled,
                    modifier = Modifier.width(240.dp),
                )
                FilterChip(selected = !viewModel.bestOf3, onClick = { viewModel.selectBestOf3(false) }, label = { Text("Bo1") }, enabled = enabled)
                FilterChip(selected = viewModel.bestOf3, onClick = { viewModel.selectBestOf3(true) }, label = { Text("Bo3") }, enabled = enabled)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                for (days in SHOWDOWN_REPLAYS_PERIODS) {
                    FilterChip(
                        selected = viewModel.days == days,
                        onClick = { viewModel.days = days },
                        label = { Text(if (days == 1) "Last day" else "Last $days days") },
                        enabled = enabled,
                    )
                }
                OutlinedTextField(
                    value = viewModel.minRatingText,
                    onValueChange = { text -> viewModel.minRatingText = text.filter(Char::isDigit).take(4) },
                    label = { Text("Minimum Elo (optional)") },
                    singleLine = true,
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(220.dp),
                )
            }
            Button(onClick = { viewModel.listReplays() }, enabled = enabled) {
                Text("Find replays")
            }
            LoadStatus(viewModel)
        }
    }
}

@Composable
private fun LoadStatus(viewModel: ShowdownReplaysViewModel) {
    when (val load = viewModel.load) {
        ShowdownReplaysLoad.Idle, is ShowdownReplaysLoad.Listed -> Unit
        is ShowdownReplaysLoad.Listing -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text("Listing the replays… ${load.found} found", Modifier.weight(1f, fill = false))
            TextButton(onClick = viewModel::cancel) { Text("Cancel") }
        }
        is ShowdownReplaysLoad.Downloading -> Column {
            LinearProgressIndicator(
                progress = { if (load.total == 0) 0f else load.done.toFloat() / load.total },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Downloading ${load.done} / ${load.total}" + if (load.failed > 0) " (${load.failed} failed)" else "",
                    Modifier.weight(1f),
                )
                TextButton(onClick = viewModel::cancel) { Text("Cancel") }
            }
        }
        is ShowdownReplaysLoad.Done -> Text(
            buildString {
                append(if (load.saved == 1) "1 replay saved" else "${load.saved} replays saved")
                if (load.failed > 0) append(", ${load.failed} couldn't be downloaded")
            }
        )
        is ShowdownReplaysLoad.Failed -> Text(load.message, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun LoadConfirmationDialog(viewModel: ShowdownReplaysViewModel) {
    val listed = viewModel.load as? ShowdownReplaysLoad.Listed ?: return
    val count = listed.toDownload.size
    val skipped = if (listed.alreadySaved > 0) " ${listed.alreadySaved} already saved are skipped." else ""
    if (count == 0) {
        AlertDialog(
            onDismissRequest = viewModel::dismissResult,
            title = { Text("No new replay") },
            text = { Text("There's no replay to download for this period.$skipped") },
            confirmButton = { TextButton(onClick = viewModel::dismissResult) { Text("OK") } },
        )
        return
    }
    AlertDialog(
        onDismissRequest = viewModel::cancel,
        title = { Text("Download replays") },
        text = {
            Text(
                (if (count == 1) "1 replay" else "$count replays") +
                    " will be downloaded and saved on this device.$skipped"
            )
        },
        confirmButton = { TextButton(onClick = viewModel::download) { Text("Download") } },
        dismissButton = { TextButton(onClick = viewModel::cancel) { Text("Cancel") } },
    )
}

@Composable
private fun ShowdownReplayCard(replay: ShowdownReplaySummary, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    MyCard(modifier = modifier, onClick = { uriHandler.openUri(replay.url) }) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                listOfNotNull(
                    formatReplayDate(replay.uploadTime),
                    replay.rating?.let { "Rating $it" },
                    replay.bestOf?.let { "Game ${it.game} of ${it.of}" },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.alpha(0.8f),
            )
            if (LocalIsCompact.current) {
                for (player in replay.players) {
                    ShowdownReplayPlayerRow(replay, player, Modifier.fillMaxWidth())
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    for (player in replay.players) {
                        ShowdownReplayPlayerRow(replay, player, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ShowdownReplayPlayerRow(replay: ShowdownReplaySummary, player: ShowdownReplayPlayer, modifier: Modifier = Modifier) {
    val hasWon = replay.winner?.matches(player.name) == true
    Column(modifier) {
        Text(
            buildString {
                append(player.name.value)
                player.beforeElo?.let { append(" ($it)") }
                if (hasWon) append(" · Winner")
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (hasWon) FontWeight.Bold else FontWeight.Normal,
        )
        PokemonTeamPreview(player.team, fillWidth = true)
    }
}
