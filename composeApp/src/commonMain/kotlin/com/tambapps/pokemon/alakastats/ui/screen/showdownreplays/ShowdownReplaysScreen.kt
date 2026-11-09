package com.tambapps.pokemon.alakastats.ui.screen.showdownreplays

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.delete
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplayPlayer
import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplaySummary
import com.tambapps.pokemon.alakastats.domain.model.GameOutcome
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.composables.ExpansionTile
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
            if (series.hiddenGames > 0) {
                Text(
                    text = if (series.hiddenGames == 1) "1 game hidden by the filters" else "${series.hiddenGames} games hidden by the filters",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
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
