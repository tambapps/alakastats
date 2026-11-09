package com.tambapps.pokemon.alakastats.ui.screen.showdownreplays

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.delete
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplayPlayer
import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplaySummary
import com.tambapps.pokemon.alakastats.domain.model.GameOutcome
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.composables.ExpansionTile
import com.tambapps.pokemon.alakastats.ui.composables.FormatSelector
import com.tambapps.pokemon.alakastats.ui.composables.cardGradientColors
import com.tambapps.pokemon.alakastats.ui.composables.withMegaForms
import com.tambapps.pokemon.alakastats.ui.composables.GameOutcomeCard
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.PokemonTeamPreview
import com.tambapps.pokemon.alakastats.ui.model.ShowdownReplayListItem
import com.tambapps.pokemon.alakastats.ui.model.formatReplayDate
import com.tambapps.pokemon.alakastats.ui.model.groupedBySeries
import com.tambapps.pokemon.alakastats.ui.screen.replayviewer.HeaderIconButton
import com.tambapps.pokemon.alakastats.ui.screen.replayviewer.ReplayViewerDialog
import com.tambapps.pokemon.alakastats.ui.service.FacingDirection
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import org.jetbrains.compose.resources.painterResource
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
    val grouped = viewModel.bestOf3 && viewModel.groupBySeries
    val listItems = remember(viewModel.savedReplays, grouped) {
        if (grouped) groupedBySeries(viewModel.savedReplays)
        else viewModel.savedReplays.map(ShowdownReplayListItem::Single)
    }
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
            Row(Modifier.widthIn(max = MAX_CONTENT_WIDTH).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        viewModel.isLoadingSaved -> "Loading the saved replays…"
                        viewModel.savedReplays.isEmpty() -> "No replay saved for this format yet"
                        else -> "${viewModel.savedReplays.size} saved replays"
                    },
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
        items(listItems, key = { it.key }) { item ->
            val itemModifier = Modifier.widthIn(max = MAX_CONTENT_WIDTH).fillMaxWidth()
            when (item) {
                is ShowdownReplayListItem.Single -> ShowdownReplayCard(item.replay, { viewModel.viewedReplay = item.replay }, itemModifier)
                is ShowdownReplayListItem.Series -> ShowdownReplaySeriesCard(item, { viewModel.viewedReplay = it }, itemModifier)
            }
        }
    }
    LoadConfirmationDialog(viewModel)
    DeletionConfirmationDialog(viewModel)
    viewModel.viewedReplay?.let { replay -> ShowdownReplayViewer(viewModel, replay) }
    ReplayDeletionConfirmationDialog(viewModel)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LoadReplaysCard(viewModel: ShowdownReplaysViewModel, modifier: Modifier = Modifier) {
    val enabled = !viewModel.isBusy
    MyCard(modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Load public replays",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { viewModel.askDeletion() },
                    enabled = enabled && viewModel.savedReplays.isNotEmpty(),
                ) {
                    Icon(painterResource(Res.drawable.delete), contentDescription = "Delete the saved replays")
                }
            }
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
private fun DeletionConfirmationDialog(viewModel: ShowdownReplaysViewModel) {
    val replays = viewModel.replaysToDelete ?: return
    val selection = buildString {
        append(viewModel.format.displayedName)
        append(if (viewModel.bestOf3) " Bo3" else " Bo1")
        append(if (viewModel.days == 1) ", last day" else ", last ${viewModel.days} days")
        viewModel.minRating?.let { append(", Elo $it or more") }
    }
    if (replays.isEmpty()) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeletion,
            title = { Text("No replay to delete") },
            text = { Text("No saved replay matches $selection.") },
            confirmButton = { TextButton(onClick = viewModel::dismissDeletion) { Text("OK") } },
        )
        return
    }
    AlertDialog(
        onDismissRequest = viewModel::dismissDeletion,
        title = { Text("Delete replays") },
        text = {
            Text(
                (if (replays.size == 1) "1 saved replay" else "${replays.size} saved replays") +
                    " of $selection will be deleted from this device. They can be downloaded again."
            )
        },
        confirmButton = {
            TextButton(onClick = viewModel::confirmDeletion) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = viewModel::dismissDeletion) { Text("Cancel") } },
    )
}

/**
 * @param showSelection whether the players show the Pokémon they brought rather than their team, e.g. the games of a
 * series, whose card shows the teams
 */
@Composable
private fun ShowdownReplayCard(
    replay: ShowdownReplaySummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showSelection: Boolean = false,
) {
    MyCard(modifier = modifier, onClick = onClick) {
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
            PlayersRows(replay.players, LocalIsCompact.current, showSelection) { player ->
                replay.winner?.matches(player.name) == true
            }
        }
    }
}

/**
 * Closed, the series takes the place of a replay: its players, its score and their teams. Opened, its games
 */
@Composable
private fun ShowdownReplaySeriesCard(
    series: ShowdownReplayListItem.Series,
    onGameClick: (ShowdownReplaySummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCompact = LocalIsCompact.current
    val winner = series.winner
    ExpansionTile(
        modifier = modifier,
        title = {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // its games are played the same day: they don't repeat it
                val date = series.games.map { it.uploadTime }.filter { it > 0 }.minOrNull()?.let { formatReplayDate(it) }
                Text(
                    listOfNotNull(
                        "Best-of-${series.bestOf} · ${series.wins.joinToString("–")}",
                        "incomplete".takeIf { series.missingGames },
                        date,
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.alpha(0.8f),
                )
                PlayersRows(series.players, isCompact, showSelection = false) { player ->
                    winner?.matches(player.name) == true
                }
            }
        },
        menu = null,
        gradientBackgroundColors = cardGradientColors,
        // its games have one, telling them apart
        bordered = false,
        // on desktop, its games are cards too: a click between them mustn't close it. On mobile, a tap anywhere does
        disableWhenOpened = !isCompact,
    ) {
        Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            for (game in series.games) {
                ShowdownReplayCard(game, { onGameClick(game) }, Modifier.fillMaxWidth(), showSelection = true)
            }
        }
    }
}

/** The players side by side on desktop, one below the other on mobile */
@Composable
private fun PlayersRows(
    players: List<ShowdownReplayPlayer>,
    isCompact: Boolean,
    showSelection: Boolean,
    hasWon: (ShowdownReplayPlayer) -> Boolean,
) {
    if (isCompact) {
        players.forEachIndexed { index, player ->
            ShowdownReplayPlayerRow(player, index, hasWon(player), showSelection, Modifier.fillMaxWidth())
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            players.forEachIndexed { index, player ->
                ShowdownReplayPlayerRow(player, index, hasWon(player), showSelection, Modifier.weight(1f))
            }
        }
    }
}

// the players' teams face each other, the first one being on the left
private fun facingDirectionOf(playerIndex: Int) = if (playerIndex == 0) FacingDirection.RIGHT else FacingDirection.LEFT

@Composable
private fun ShowdownReplayPlayerRow(
    player: ShowdownReplayPlayer,
    playerIndex: Int,
    hasWon: Boolean,
    showSelection: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (hasWon) {
                GameOutcomeCard(GameOutcome.WIN, small = true)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                buildString {
                    append(player.name.value)
                    player.beforeElo?.let { append(" ($it)") }
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (hasWon) FontWeight.Bold else FontWeight.Normal,
            )
        }
        val facingDirection = facingDirectionOf(playerIndex)
        // e.g. none brought in a game forfeited at the team preview
        if (showSelection && player.brought.isNotEmpty()) {
            // the Mega Evolutions' sprites, without the items
            val selection = withMegaForms(player.brought, player.broughtItems).map { it.first }
            // the leads in the middle, facing each other: on the right for the first player, on the left for the second
            val ordered = if (playerIndex == 0) selection.asReversed() else selection
            PokemonTeamPreview(ordered, fillWidth = true, facingDirection = facingDirection)
        } else {
            // with, on desktop, their items, and the Mega Evolutions' sprites
            PokemonTeamPreview(player.team, player.items, fillWidth = true, facingDirection = facingDirection)
        }
    }
}
