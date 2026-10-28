package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.replay

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.GameOutcome
import com.tambapps.pokemon.alakastats.domain.model.Player
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.getGameOutcome
import com.tambapps.pokemon.alakastats.domain.model.isInternalMatch
import com.tambapps.pokemon.alakastats.domain.model.getPlayers
import com.tambapps.pokemon.alakastats.ui.composables.ExpansionTile
import com.tambapps.pokemon.alakastats.ui.composables.GameOutcomeCard
import com.tambapps.pokemon.alakastats.ui.composables.PokemonTeamPreview
import com.tambapps.pokemon.alakastats.ui.model.ReplayListItem
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.FiltersBar
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.Header
import com.tambapps.pokemon.alakastats.ui.theme.tabReplaysTextMarginTopMobile
import com.tambapps.pokemon.alakastats.ui.theme.teamlyticsTabPaddingBottom
import com.tambapps.pokemon.util.MegaUtils.toMega

@Composable
internal fun TeamReplayTabMobile(viewModel: TeamReplayViewModel, scrollState: LazyListState) {
    val team = viewModel.team
    val items = viewModel.replayItems
    LaunchedEffect(viewModel.useCase.filters, viewModel.sort, viewModel.groupBySeries) {
        scrollState.scrollToItem(index = 0, scrollOffset = 0)
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        state = scrollState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(Modifier.height(tabReplaysTextMarginTopMobile))
            FiltersBar(viewModel, headerActions = {
                GroupBySeriesChip(viewModel, Modifier.padding(end = 8.dp))
                ReplaySortChip(viewModel.sort, viewModel::applySort)
            })
            Spacer(Modifier.height(16.dp))
        }

        item {
            Header(viewModel.useCase)
        }

        item {
            ReloadUngroupedSeriesHint(viewModel, Modifier.padding(vertical = 8.dp))
        }

        itemsIndexed(items) { index, item ->
            ReplayListItemView(team, item) { replay ->
                MobileReplay(viewModel, team, replay, inSeries = item is ReplayListItem.Series)
            }
            if (index < items.size - 1) {
                Spacer(Modifier.height(32.dp))
            }
        }

        item {
            // just to be able to scroll past Fab button
            Spacer(Modifier.height(teamlyticsTabPaddingBottom))
        }
    }

}

@Composable
private fun MobileSdNamesWarning(viewModel: TeamReplayViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text("⚠\uFE0F\nYour showdown names didn't match with any player of this game",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(4.dp))
        EditSdNamesButton(viewModel)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MobileReplay(viewModel: TeamReplayViewModel, team: Teamlytics, replay: ReplayAnalytics, inSeries: Boolean) {
    ReplayCompact(team, replay, viewModel, inSeries = inSeries)
}

/**
 * @param inSeries shown in its series' card: titled with its number rather than the opponent the series' card
 * already tells
 */
@Composable
fun ReplayCompact(
    team: Teamlytics,
    replay: ReplayAnalytics,
    viewModel: TeamReplayViewModel? = null,
    onClick: (() -> Unit)? = null,
    gradientBackgroundColors: List<Color>? = null,
    borderColor: Color? = null,
    inSeries: Boolean = false,
    ) {
    val (currentPlayer, opponentPlayer) = team.getPlayers(replay)
    val gameOutput = team.getGameOutcome(replay)
    ExpansionTile(
        title = { isExpanded ->
            GameOutcomeCard(gameOutput)
            VsText(currentPlayer, opponentPlayer, gameOutput, team.isInternalMatch(replay), replay.bestOf, inSeries = inSeries)
        },
        subtitle = {
            // in a series, its card shows the team
            if (gameOutput != GameOutcome.UNKNOWN && !inSeries) {
                PokemonTeamPreview(opponentPlayer, fillWidth = true)
            }
        },
        menu = if (viewModel != null) ({ isMenuExpandedState ->
            ReplayDropDownMenu(isMenuExpandedState, viewModel, replay)
        }) else null,
        onClick=onClick,
        gradientBackgroundColors=gradientBackgroundColors,
        borderColor=borderColor
    ) {
        Column {
            Spacer(Modifier.height(8.dp))
            // in a series, its card has the sheet
            val ots = opponentPlayer.ots?.takeIf { !inSeries }
            if (gameOutput != GameOutcome.UNKNOWN && ots != null && replay.url != null) {
                Row(Modifier.fillMaxWidth()
                    .padding(horizontal = 8.dp)) {
                    Spacer(Modifier.weight(1f))
                    OtsButton(team.format, opponentPlayer, ots)
                    Spacer(Modifier.width(32.dp))
                    ViewReplayButton(team, replay, replay.url)
                    Spacer(Modifier.weight(1f))
                }
            } else if (gameOutput != GameOutcome.UNKNOWN && ots != null) {
                OtsButton(team.format, opponentPlayer, ots, modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (replay.url != null) {
                ViewReplayButton(team, replay, replay.url, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            Spacer(Modifier.height(8.dp))

            if (viewModel != null && gameOutput == GameOutcome.UNKNOWN) {
                MobileSdNamesWarning(viewModel)
            } else {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    MobilePlayer(
                        modifier = Modifier.weight(1f),
                        player = currentPlayer,
                        playerName = "You",
                        isYouPlayer = true
                    )

                    MobilePlayer(
                        modifier = Modifier.weight(1f),
                        player = opponentPlayer,
                        playerName = "Opponent",
                    )
                }
            }

            if (!replay.notes.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    replay.notes,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun MobilePlayer(modifier: Modifier, player: Player, playerName: String, isYouPlayer: Boolean = false) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlayerNameEloText(player, playerName)
        Spacer(Modifier.height(8.dp))

        for (pokemon in player.selection) {
            val teraType = player.terastallization?.takeIf { it.pokemon.matches(pokemon) }?.type
            val megaPokemon = player.megaEvolution
                ?.takeIf { it.pokemon.baseMatches(pokemon) }
                ?.let { it.pokemon.toMega(it.item) }
            SelectedPokemon(
                pokemon = pokemon,
                teraType = teraType,
                isYouPlayer = isYouPlayer,
                megaPokemon = megaPokemon
            )
        }
    }
}
