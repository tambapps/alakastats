package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.replay

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.more_vert
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.domain.model.GameOutput
import com.tambapps.pokemon.alakastats.domain.model.Player
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.getGameOutput
import com.tambapps.pokemon.alakastats.domain.model.getPlayers
import com.tambapps.pokemon.alakastats.ui.composables.GameOutputCard
import com.tambapps.pokemon.alakastats.ui.composables.LinearProgressBar
import com.tambapps.pokemon.alakastats.ui.composables.PokemonTeamPreview
import com.tambapps.pokemon.alakastats.ui.screen.editteam.EditTeamScreen
import com.tambapps.pokemon.alakastats.ui.theme.defaultIconColor
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun TeamReplayTabDesktop(viewModel: TeamReplayViewModel) {
    val team = viewModel.team

    val replays = team.replays
    if (replays.isEmpty() && !viewModel.isLoading) {
        NoReplaysDesktop(viewModel)
        return
    }
    Column(Modifier.fillMaxSize()) {
        if (viewModel.isLoading) {
            LinearProgressBar()
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Header(viewModel, replays, team)
            }
            itemsIndexed(replays) { index, replay ->
                DesktopReplay(viewModel, team, replay)
                if (index < replays.size - 1) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Spacer(Modifier.width(64.dp))
                        HorizontalDivider(Modifier.weight(1f).height(2.dp))
                        Spacer(Modifier.width(64.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopReplay(viewModel: TeamReplayViewModel, team: Teamlytics, replay: ReplayAnalytics) {
    val (currentPlayer, opponentPlayer) = team.getPlayers(replay)
    val gameOutput = team.getGameOutput(replay)
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            GameOutputCard(gameOutput)
            VsText(currentPlayer, opponentPlayer, gameOutput)
            Spacer(Modifier.width(16.dp))
            if (gameOutput != GameOutput.UNKNOWN) {
                PokemonTeamPreview(viewModel.pokemonImageService, opponentPlayer,
                    childModifier = Modifier.size(100.dp).padding(bottom = 16.dp))

                opponentPlayer.ots?.let { openTeamSheet ->
                    Spacer(Modifier.width(16.dp))
                    OtsButton(opponentPlayer, opponentPlayer.ots, viewModel)
                }
            }
            replay.url?.let { replayUrl ->
                Spacer(Modifier.width(20.dp))
                ViewReplayButton(team, replay, replayUrl)
            }
            Spacer(Modifier.weight(1f))

            val isMenuExpandedState = remember { mutableStateOf(false) }
            IconButton(onClick = { isMenuExpandedState.value = !isMenuExpandedState.value }) {
                Icon(
                    modifier = Modifier.size(64.dp),
                    painter = painterResource(Res.drawable.more_vert),
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.defaultIconColor
                )
                ReplayDropDownMenu(isMenuExpandedState, viewModel, replay)
            }
            Spacer(Modifier.width(8.dp))
        }

        if (gameOutput == GameOutput.UNKNOWN) {
            DesktopSdNamesWarning(viewModel)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                DesktopPlayer(modifier = Modifier.weight(1f), player = currentPlayer, playerName = "You", viewModel = viewModel)
                Spacer(Modifier.width(8.dp))
                DesktopPlayer(modifier = Modifier.weight(1f), player = opponentPlayer, playerName = "Opponent", viewModel = viewModel)
            }
        }

        if (!replay.notes.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                replay.notes,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DesktopSdNamesWarning(viewModel: TeamReplayViewModel) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⚠\uFE0F your showdown names didn't match with any player of this game", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(16.dp))
        EditSdNamesButton(viewModel)
    }
}

@Composable
internal fun EditSdNamesButton(viewModel: TeamReplayViewModel) {
    val navigator = LocalNavigator.currentOrThrow

    OutlinedButton(onClick = { navigator.push(EditTeamScreen(viewModel.team))}) {
        Text("Edit Showdown Names")
    }
}
@Composable
private fun DesktopPlayer(modifier: Modifier, player: Player, playerName: String, viewModel: TeamReplayViewModel) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(playerName)
        Spacer(Modifier.height(16.dp))

        if (player.beforeElo != null && player.afterElo != null) {
            Text("Elo: ${player.beforeElo} -> ${player.afterElo}")
        } else if (player.beforeElo != null) {
            Text("Elo: ${player.beforeElo}")
        }

        Row {
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
}

@Composable
private fun NoReplaysDesktop(viewModel: TeamReplayViewModel) {
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
                    .align(Alignment.TopStart)
            )
        }
    }
}

@Composable
private fun VsText(currentPlayer: Player, opponentPlayer: Player, gameOutput: GameOutput) {
    val text =
        if (gameOutput != GameOutput.UNKNOWN) "VS ${opponentPlayer.name}"
        else "${currentPlayer.name} VS ${opponentPlayer.name}"
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 8.dp)
    )
}