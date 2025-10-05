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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.Player
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.getGameOutput
import com.tambapps.pokemon.alakastats.domain.model.getPlayers
import com.tambapps.pokemon.alakastats.ui.composables.ExpansionTile
import com.tambapps.pokemon.alakastats.ui.composables.GameOutputCard
import com.tambapps.pokemon.alakastats.ui.composables.LinearProgressBar
import com.tambapps.pokemon.alakastats.ui.composables.PokemonTeamPreview

@Composable
internal fun TeamReplayTabMobile(viewModel: TeamReplayViewModel) {
    val team = viewModel.team

    if (team.replays.isEmpty() && !viewModel.isLoading) {
        NoReplaysMobile(viewModel)
        return
    }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            item {
                AddReplayButton(viewModel)
                Spacer(Modifier.height(32.dp))
            }
            val replays = team.replays
            itemsIndexed(replays) { index, replay ->
                MobileReplay(viewModel, team, replay)
                if (index < replays.size - 1) {
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
        if (viewModel.isLoading) {
            LinearProgressBar()
        }
    }
}

@Composable
private fun MobileReplay(viewModel: TeamReplayViewModel, team: Teamlytics, replay: ReplayAnalytics) {
    val (currentPlayer, opponentPlayer) = team.getPlayers(replay)

    ExpansionTile(
        title = { isExpanded ->
            val gameOutput = team.getGameOutput(replay)
            GameOutputCard(gameOutput)
            VsText(opponentPlayer)
        },
        subtitle = {
            PokemonTeamPreview(viewModel.pokemonImageService, opponentPlayer, fillWidth = true)
        },
        menu = { isMenuExpandedState ->
            ReplayDropDownMenu(isMenuExpandedState, viewModel, replay)
        },
    ) {
        Column {
            Spacer(Modifier.height(8.dp))
            if (opponentPlayer.ots != null && replay.url != null) {
                Row(Modifier.fillMaxWidth()
                    .padding(horizontal = 8.dp)) {
                    Spacer(Modifier.weight(1f))
                    OtsButton(opponentPlayer, opponentPlayer.ots, viewModel)
                    Spacer(Modifier.width(32.dp))
                    ViewReplayButton(team, replay, replay.url)
                    Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            } else if (opponentPlayer.ots != null) {
                OtsButton(opponentPlayer, opponentPlayer.ots, viewModel)
                Spacer(Modifier.height(8.dp))
            } else if (replay.url != null) {
                ViewReplayButton(team, replay, replay.url)
                Spacer(Modifier.height(8.dp))
            }

            Row(Modifier.fillMaxWidth()) {
                MobilePlayer(
                    modifier = Modifier.weight(1f),
                    player = currentPlayer,
                    playerName = "You",
                    viewModel = viewModel
                )

                MobilePlayer(
                    modifier = Modifier.weight(1f),
                    player = opponentPlayer,
                    playerName = "Opponent",
                    viewModel = viewModel
                )
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
private fun MobilePlayer(modifier: Modifier, player: Player, playerName: String, viewModel: TeamReplayViewModel) {
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
                pokemonImageService = viewModel.pokemonImageService
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
            AddReplayButton(viewModel)
        }

        if (viewModel.isLoading) {
            LinearProgressBar(
                modifier = Modifier
                    .align(Alignment.BottomStart)
            )
        }
    }
}