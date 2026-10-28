package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.replay

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
 * A replay, or the games of a series in a frame with the series' score
 *
 * @param game how a game is shown, as when not grouped
 */
@Composable
internal fun ReplayListItemView(
    team: Teamlytics,
    item: ReplayListItem,
    game: @Composable (ReplayAnalytics) -> Unit,
) {
    when (item) {
        is ReplayListItem.Single -> game(item.replay)
        is ReplayListItem.Series -> SeriesView(team, item, game)
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
    game: @Composable (ReplayAnalytics) -> Unit,
) {
    val opponent = team.getPlayers(series.games.first()).second
    // usually in every game, when the format has open team sheets
    val sheetOwner = series.games.map { team.getPlayers(it).second }.firstOrNull { it.ots != null }
    val isCompact = LocalIsCompact.current
    ExpansionTile(
        title = {
            GameOutcomeCard(series.outcome)
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
                text = if (count == 1) "1 replay was added with an older version of the app: reload it to see its score, turns and KOs$toGroup"
                else "$count replays were added with an older version of the app: reload them to see their score, turns and KOs$toGroup",
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
