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
import com.tambapps.pokemon.alakastats.domain.model.getPlayers
import com.tambapps.pokemon.alakastats.ui.composables.ExpansionTile
import com.tambapps.pokemon.alakastats.ui.composables.GameOutcomeCard
import com.tambapps.pokemon.alakastats.ui.composables.PokemonTeamPreview
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.FiltersBar
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.Header
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.theme.tabReplaysTextMarginTopMobile
import com.tambapps.pokemon.alakastats.ui.theme.teamlyticsTabPaddingBottom

@Composable
internal fun TeamReplayTabMobile(viewModel: TeamReplayViewModel, scrollState: LazyListState) {
    val team = viewModel.team
    LaunchedEffect(viewModel.useCase.filters) {
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
            FiltersBar(viewModel)
            Spacer(Modifier.height(16.dp))
        }

        item {
            Header(viewModel.useCase)
        }

        itemsIndexed(team.replays) { index, replay ->
            MobileReplay(viewModel, team, replay)
            if (index < team.replays.size - 1) {
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
private fun MobileReplay(viewModel: TeamReplayViewModel, team: Teamlytics, replay: ReplayAnalytics) {
    ReplayCompact(team, replay, viewModel.pokemonImageService, viewModel)
}

@Composable
fun ReplayCompact(
    team: Teamlytics,
    replay: ReplayAnalytics,
    pokemonImageService: PokemonImageService,
    viewModel: TeamReplayViewModel? = null,
    onClick: (() -> Unit)? = null,
    gradientBackgroundColors: List<Color>? = null,
    borderColor: Color? = null
    ) {
    val (currentPlayer, opponentPlayer) = team.getPlayers(replay)
    val gameOutput = team.getGameOutcome(replay)
    ExpansionTile(
        title = { isExpanded ->
            GameOutcomeCard(gameOutput)
            VsText(currentPlayer, opponentPlayer, gameOutput)
        },
        subtitle = {
            if (gameOutput != GameOutcome.UNKNOWN) {
                PokemonTeamPreview(pokemonImageService, opponentPlayer, fillWidth = true)
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
            if (gameOutput != GameOutcome.UNKNOWN && opponentPlayer.ots != null && replay.url != null) {
                Row(Modifier.fillMaxWidth()
                    .padding(horizontal = 8.dp)) {
                    Spacer(Modifier.weight(1f))
                    OtsButton(team.format, opponentPlayer, opponentPlayer.ots, pokemonImageService)
                    Spacer(Modifier.width(32.dp))
                    ViewReplayButton(team, replay, replay.url)
                    Spacer(Modifier.weight(1f))
                }
            } else if (gameOutput != GameOutcome.UNKNOWN && opponentPlayer.ots != null) {
                OtsButton(team.format, opponentPlayer, opponentPlayer.ots, pokemonImageService, modifier = Modifier.align(Alignment.CenterHorizontally))
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
                        pokemonImageService = pokemonImageService,
                        isYouPlayer = true
                    )

                    MobilePlayer(
                        modifier = Modifier.weight(1f),
                        player = opponentPlayer,
                        playerName = "Opponent",
                        pokemonImageService = pokemonImageService
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
private fun MobilePlayer(modifier: Modifier, player: Player, playerName: String, pokemonImageService: PokemonImageService, isYouPlayer: Boolean = false) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlayerNameEloText(player, playerName)
        Spacer(Modifier.height(8.dp))

        for (pokemon in player.selection) {
            val teraType = player.terastallization?.takeIf { it.pokemon.matches(pokemon) }?.type
            SelectedPokemon(
                pokemon = pokemon,
                teraType = teraType,
                pokemonImageService = pokemonImageService,
                isYouPlayer = isYouPlayer
            )
        }
    }
}
