package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.replay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.GameOutput
import com.tambapps.pokemon.alakastats.domain.model.Player
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.getGameOutput
import com.tambapps.pokemon.alakastats.domain.model.getPlayers
import com.tambapps.pokemon.alakastats.ui.composables.ExpansionTile
import com.tambapps.pokemon.alakastats.ui.composables.GameOutputCard
import com.tambapps.pokemon.alakastats.ui.composables.LinearProgressBarIfEnabled
import com.tambapps.pokemon.alakastats.ui.composables.PokemonTeamPreview

@Composable
internal fun TeamReplayTabMobile(viewModel: TeamReplayViewModel) {
    val team = viewModel.team
    val replays = team.replays

    if (viewModel.hasNoReplaysToShow) {
        NoReplaysMobile(viewModel)
        return
    }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Header(replays, team)
            }
            itemsIndexed(replays) { index, replay ->
                MobileReplay(viewModel, team, replay)
                if (index < replays.size - 1) {
                    Spacer(Modifier.height(32.dp))
                }
            }

            item {
                // just to be able to scroll past Fab button
                Spacer(Modifier.height(64.dp))
            }
        }
        LinearProgressBarIfEnabled(viewModel.isLoading)
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
    val (currentPlayer, opponentPlayer) = team.getPlayers(replay)

    val gameOutput = team.getGameOutput(replay)
    ExpansionTile(
        title = { isExpanded ->
            GameOutputCard(gameOutput)
            VsText(currentPlayer, opponentPlayer, gameOutput)
        },
        subtitle = {
            if (gameOutput != GameOutput.UNKNOWN) {
                PokemonTeamPreview(viewModel.pokemonImageService, opponentPlayer, fillWidth = true)
            }
        },
        menu = { isMenuExpandedState ->
            ReplayDropDownMenu(isMenuExpandedState, viewModel, replay)
        },
    ) {
        Column {
            Spacer(Modifier.height(8.dp))
            if (gameOutput != GameOutput.UNKNOWN && opponentPlayer.ots != null && replay.url != null) {
                Row(Modifier.fillMaxWidth()
                    .padding(horizontal = 8.dp)) {
                    Spacer(Modifier.weight(1f))
                    OtsButton(opponentPlayer, opponentPlayer.ots, viewModel)
                    Spacer(Modifier.width(32.dp))
                    ViewReplayButton(team, replay, replay.url)
                    Spacer(Modifier.weight(1f))
                }
            } else if (gameOutput != GameOutput.UNKNOWN && opponentPlayer.ots != null) {
                OtsButton(opponentPlayer, opponentPlayer.ots, viewModel, modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (replay.url != null) {
                ViewReplayButton(team, replay, replay.url, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            Spacer(Modifier.height(8.dp))

            if (gameOutput == GameOutput.UNKNOWN) {
                MobileSdNamesWarning(viewModel)
            } else {
                Row(Modifier.fillMaxWidth()) {
                    MobilePlayer(
                        modifier = Modifier.weight(1f),
                        player = currentPlayer,
                        playerName = "You",
                        viewModel = viewModel,
                        isYouPlayer = true
                    )

                    MobilePlayer(
                        modifier = Modifier.weight(1f),
                        player = opponentPlayer,
                        playerName = "Opponent",
                        viewModel = viewModel
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
private fun MobilePlayer(modifier: Modifier, player: Player, playerName: String, viewModel: TeamReplayViewModel, isYouPlayer: Boolean = false) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(playerName)

        if (player.beforeElo != null && player.afterElo != null) {
            Text("Elo: ${player.beforeElo} -> ${player.afterElo}")
        } else if (player.beforeElo != null) {
            Text("Elo: ${player.beforeElo}")
        }

        for (pokemon in player.selection) {
            val teraType = player.terastallization?.takeIf { it.pokemon.matches(pokemon) }?.type
            SelectedPokemon(
                pokemon = pokemon,
                teraType = teraType,
                pokemonImageService = viewModel.pokemonImageService,
                isYouPlayer = isYouPlayer
            )
        }
    }
}

@Composable
private fun NoReplaysMobile(viewModel: TeamReplayViewModel) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("No replays were found")
            AddReplayTextButton(viewModel)
        }
        LinearProgressBarIfEnabled(viewModel.isLoading, modifier = Modifier
            .align(Alignment.BottomStart))
    }
}

@Composable
private fun VsText(currentPlayer: Player, opponentPlayer: Player, gameOutput: GameOutput, modifier: Modifier = Modifier) {
    val text =
        if (gameOutput != GameOutput.UNKNOWN) "VS ${opponentPlayer.name}"
        else "${currentPlayer.name}\nVS\n${opponentPlayer.name}"
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier.padding(start = 8.dp),
        textAlign = if (gameOutput != GameOutput.UNKNOWN) null else TextAlign.Center,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1
    )
}