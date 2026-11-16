package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.replay

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.close
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.ui.state.ToggleableState
import com.tambapps.pokemon.alakastats.ui.composables.CopyReplayUrlsButton
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.GameOutcome
import com.tambapps.pokemon.alakastats.domain.model.Player
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.getPlayers
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.composables.ExpansionTile
import com.tambapps.pokemon.alakastats.ui.composables.GameOutcomeCard
import com.tambapps.pokemon.alakastats.ui.composables.PokemonTeamPreview
import com.tambapps.pokemon.alakastats.ui.composables.cardGradientColors
import com.tambapps.pokemon.alakastats.ui.model.ReplayListItem
import com.tambapps.pokemon.alakastats.ui.model.formatReplayDate
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.rememberGuardedChange
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact

@Composable
internal fun GroupBySeriesChip(viewModel: TeamReplayViewModel, modifier: Modifier = Modifier) {
    if (!viewModel.hasSeries) return
    FilterChip(
        selected = viewModel.groupBySeries,
        onClick = { viewModel.updateGroupBySeries(!viewModel.groupBySeries) },
        label = { Text("Group by Bo3", maxLines = 1) },
        modifier = modifier,
    )
}

/**
 * Above the replays: where the selection bar is while selecting, so that the list doesn't move when it starts
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun LazyListScope.replaySelectionItem(viewModel: TeamReplayViewModel) {
    if (viewModel.isSelecting) {
        // stays at the top while scrolling: the selection's actions are at hand
        stickyHeader(key = "selection") { ReplaySelectionBar(viewModel, Modifier.padding(bottom = 8.dp)) }
    } else {
        item(key = "selection") {
            // not in the filters' title, whose title would no longer fit on mobile
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = viewModel::startSelection, enabled = viewModel.canSelect) { Text("Select") }
            }
        }
    }
}

/**
 * While selecting: how many are selected, selecting them all, and the actions on them
 */
@Composable
private fun ReplaySelectionBar(viewModel: TeamReplayViewModel, modifier: Modifier = Modifier) {
    val count = viewModel.selectedRefs.size
    Row(
        modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = viewModel::stopSelection) {
            Icon(painterResource(Res.drawable.close), contentDescription = "Stop selecting")
        }
        Text(
            if (count == 1) "1 selected" else "$count selected",
            style = MaterialTheme.typography.titleMedium,
        )
        TextButton(onClick = viewModel::toggleSelectAll) {
            Text(if (viewModel.areAllShownSelected) "Unselect all" else "Select all")
        }
        Spacer(Modifier.weight(1f))
        CopyReplayUrlsButton(urls = viewModel::selectedUrls, enabled = count > 0)
    }
}

/**
 * A replay, or the games of a series in a frame with the series' score
 *
 * @param game how a game is shown, as when not grouped
 * @param selectedRefs the selected replays, null when not selecting: the series' checkbox then selects its games
 */
@Composable
internal fun ReplayListItemView(
    team: Teamlytics,
    item: ReplayListItem,
    selectedRefs: Set<String>? = null,
    onSeriesSelectionToggle: (ReplayListItem.Series) -> Unit = {},
    game: @Composable (ReplayAnalytics) -> Unit,
) {
    when (item) {
        is ReplayListItem.Single -> game(item.replay)
        is ReplayListItem.Series -> SeriesView(team, item, selectedRefs, { onSeriesSelectionToggle(item) }, game)
    }
}

/**
 * Closed, the series takes the place of a replay: its score and the opponent's team. Opened, its games
 *
 * The opponent's team and sheet are the same for all the games of a series: the series' card shows them, not its games
 */
@Composable
private fun SeriesView(
    team: Teamlytics,
    series: ReplayListItem.Series,
    selectedRefs: Set<String>?,
    onSelectionToggle: () -> Unit,
    game: @Composable (ReplayAnalytics) -> Unit,
) {
    val opponent = team.getPlayers(series.games.first()).second
    // usually in every game, when the format has open team sheets
    val sheetOwner = series.games.map { team.getPlayers(it).second }.firstOrNull { it.ots != null }
    val isCompact = LocalIsCompact.current
    ExpansionTile(
        title = {
            // only the games having a link can be selected
            val selectableGames = series.games.filter { it.url != null }
            if (selectedRefs != null && selectableGames.isNotEmpty()) {
                val selectedCount = selectableGames.count { it.reference in selectedRefs }
                TriStateCheckbox(
                    state = when (selectedCount) {
                        0 -> ToggleableState.Off
                        selectableGames.size -> ToggleableState.On
                        else -> ToggleableState.Indeterminate
                    },
                    onClick = onSelectionToggle,
                )
            }
            // an unknown outcome is an incomplete series, as its subtitle tells
            GameOutcomeCard(series.outcome, letter = if (series.outcome == GameOutcome.UNKNOWN) 'I' else series.outcome.name[0])
            SeriesTitle(series, opponent)
            // on the same line when there is room, below otherwise
            if (!isCompact) {
                Spacer(Modifier.width(16.dp))
                PokemonTeamPreview(opponent, modifier = Modifier.weight(1f), fillWidth = true)
                sheetOwner?.ots?.let { ots ->
                    Spacer(Modifier.width(16.dp))
                    OtsButton(team.format, sheetOwner, ots)
                }
            }
        },
        subtitle = {
            if (isCompact) {
                PokemonTeamPreview(opponent, fillWidth = true)
            }
        },
        menu = null,
        gradientBackgroundColors = cardGradientColors,
        // its games have one, telling them apart
        bordered = false,
        // on desktop, its games are cards too: a click between them mustn't close it. On mobile, a tap anywhere does
        disableWhenOpened = !isCompact,
    ) {
        Column(Modifier.padding(top = 16.dp)) {
            // on mobile, opened only: the closed card stays light
            if (isCompact) {
                sheetOwner?.ots?.let { ots ->
                    OtsButton(team.format, sheetOwner, ots, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(Modifier.height(16.dp))
                }
            }
            series.games.forEachIndexed { index, replay ->
                game(replay)
                if (index < series.games.lastIndex) {
                    Spacer(Modifier.height(16.dp))
                }
            }
            if (series.hiddenGames > 0) {
                Spacer(Modifier.height(8.dp))
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

@Composable
private fun SeriesTitle(series: ReplayListItem.Series, opponent: Player) {
    Column(Modifier.padding(start = 8.dp)) {
        Text(
            text = "VS ${opponent.name.value}",
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // its games are played the same day: they don't repeat it
        val date = series.games.map { it.uploadTime }.filter { it > 0 }.minOrNull()?.let { formatReplayDate(it) }
        Text(
            // the score of the whole series, whatever the filters
            text = "Best-of-${series.bestOf} · ${series.wins}–${series.losses}" +
                    (if (series.outcome == GameOutcome.UNKNOWN) " · incomplete" else "") +
                    (date?.let { " · $it" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/**
 * The replays loaded with an older version of the app miss what it parses now (e.g. their score, or the game number
 * a best-of-3 is grouped with): they have to be reloaded
 */
@Composable
internal fun ReloadOutdatedReplaysHint(viewModel: TeamReplayViewModel, modifier: Modifier = Modifier) {
    val count = viewModel.outdatedReplays.size
    if (count == 0 || viewModel.isReadOnly) return
    val snackBar = LocalSnackBar.current
    val guardedChange = rememberGuardedChange()
    val toGroup = if (viewModel.groupBySeries && viewModel.ungroupedSeriesReplays.isNotEmpty()) ", and to group the best-of-3s" else ""
    ElevatedCard(modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (count == 1) "1 replay was added with an older version of the app: reload it to see its turns and how it ended$toGroup"
                else "$count replays were added with an older version of the app: reload them to see their turns and how they ended$toGroup",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { guardedChange { viewModel.reloadOutdatedReplays(snackBar) } },
                enabled = !viewModel.isLoading,
            ) {
                Text("Reload")
            }
        }
    }
}
