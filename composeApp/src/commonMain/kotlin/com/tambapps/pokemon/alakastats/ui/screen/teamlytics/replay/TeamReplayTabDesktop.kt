package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.replay

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.more_vert
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.tambapps.pokemon.alakastats.ui.composables.ExpansionTile
import com.tambapps.pokemon.alakastats.ui.composables.GameOutputCard
import com.tambapps.pokemon.alakastats.ui.composables.LazyColumnWithScrollbar
import com.tambapps.pokemon.alakastats.ui.composables.LinearProgressBarIfEnabled
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.PokemonTeamPreview
import com.tambapps.pokemon.alakastats.ui.composables.cardGradientColors
import com.tambapps.pokemon.alakastats.ui.screen.editteam.EditTeamScreen
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.theme.defaultIconColor
import com.tambapps.pokemon.alakastats.ui.theme.teamlyticsTabPaddingBottom
import org.jetbrains.compose.resources.painterResource
import kotlin.Boolean
import kotlin.collections.chunked
import kotlin.collections.forEach

@Composable
internal fun TeamReplayTabDesktop(viewModel: TeamReplayViewModel, scrollState: LazyListState) {
    val team = viewModel.team

    if (viewModel.hasNoReplaysToShow) {
        NoReplaysDesktop(viewModel)
        return
    }
    Column(Modifier.fillMaxSize()) {
        LinearProgressBarIfEnabled(viewModel.isLoading)
        LaunchedEffect(viewModel.useCase.filters) {
            scrollState.scrollToItem(index = 0, scrollOffset = 0)
        }
        LazyColumnWithScrollbar(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            state = scrollState,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Header(viewModel.useCase)
            }
            itemsIndexed(team.replays) { index, replay ->
                DesktopReplay(viewModel, team, replay)
                Spacer(Modifier.height(32.dp))
            }
            item {
                // just to be able to scroll past Fab button
                Spacer(Modifier.height(teamlyticsTabPaddingBottom))
            }
        }
    }
}

@Composable
fun ExpandableDesktopReplay(team: Teamlytics, replay: ReplayAnalytics, pokemonImageService: PokemonImageService) {
    val (currentPlayer, opponentPlayer) = team.getPlayers(replay)
    val gameOutput = team.getGameOutput(replay)

    ExpansionTile(
        title = {
            HeadRow(team, replay, gameOutput, currentPlayer=currentPlayer, opponentPlayer=opponentPlayer, pokemonImageService = pokemonImageService, viewModel = null)
        },
        disableWhenOpened = true
    ) {
        ReplayContent(replay, gameOutput, currentPlayer=currentPlayer, opponentPlayer=opponentPlayer, pokemonImageService, viewModel = null)
    }
}

@Composable
fun DesktopReplay(viewModel: TeamReplayViewModel, team: Teamlytics, replay: ReplayAnalytics) {
    val (currentPlayer, opponentPlayer) = team.getPlayers(replay)
    val gameOutput = team.getGameOutput(replay)
    MyCard(
        modifier = Modifier.fillMaxWidth(),
        gradientBackgroundColors = cardGradientColors
        ) {
        HeadRow(team, replay, gameOutput, currentPlayer=currentPlayer, opponentPlayer=opponentPlayer, pokemonImageService = viewModel.pokemonImageService, viewModel)
        ReplayContent(replay, gameOutput, currentPlayer=currentPlayer, opponentPlayer=opponentPlayer, viewModel.pokemonImageService, viewModel)
    }
}

@Composable
private fun HeadRow(
    team: Teamlytics,
    replay: ReplayAnalytics,
    gameOutput: GameOutput,
    currentPlayer: Player,
    opponentPlayer: Player,
    pokemonImageService: PokemonImageService,
    viewModel: TeamReplayViewModel?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        GameOutputCard(gameOutput)
        VsText(currentPlayer, opponentPlayer, gameOutput)
        Spacer(Modifier.width(16.dp))
        if (gameOutput != GameOutput.UNKNOWN) {
            PokemonTeamPreview(pokemonImageService, opponentPlayer,
                childModifier = Modifier.size(100.dp).padding(bottom = 16.dp))

            opponentPlayer.ots?.let { openTeamSheet ->
                Spacer(Modifier.width(16.dp))
                OtsButton(opponentPlayer, opponentPlayer.ots, pokemonImageService)
            }
        }
        replay.url?.let { replayUrl ->
            Spacer(Modifier.width(20.dp))
            ViewReplayButton(team, replay, replayUrl)
        }
        Spacer(Modifier.weight(1f))

        if (viewModel != null) {
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
        }
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
private fun ReplayContent(
    replay: ReplayAnalytics,
    gameOutput: GameOutput,
    currentPlayer: Player,
    opponentPlayer: Player,
    pokemonImageService: PokemonImageService,
    viewModel: TeamReplayViewModel?,
) {
    if (gameOutput == GameOutput.UNKNOWN) {
        DesktopSdNamesWarning(viewModel)
    } else {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            DesktopPlayer(modifier = Modifier.weight(1f), player = currentPlayer, playerName = "You", pokemonImageService = pokemonImageService, useAlternativeSurfaceContainer = viewModel == null, isYouPlayer = true)
            Spacer(Modifier.width(8.dp))
            DesktopPlayer(modifier = Modifier.weight(1f), player = opponentPlayer, playerName = "Opponent", pokemonImageService = pokemonImageService, useAlternativeSurfaceContainer = viewModel == null)
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

@Composable
private fun DesktopSdNamesWarning(viewModel: TeamReplayViewModel?) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⚠\uFE0F your showdown names didn't match with any player of this game", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(16.dp))
        viewModel?.let { EditSdNamesButton(it) }
    }
}

@Composable
private fun DesktopPlayer(modifier: Modifier, player: Player, playerName: String, pokemonImageService: PokemonImageService, useAlternativeSurfaceContainer: Boolean, isYouPlayer: Boolean = false) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlayerNameEloText(player, playerName)
        Spacer(Modifier.height(8.dp))
        DesktopSelection(player, pokemonImageService, useAlternativeSurfaceContainer=useAlternativeSurfaceContainer, isYouPlayer=isYouPlayer)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DesktopSelection(
    player: Player,
    pokemonImageService: PokemonImageService,
    useAlternativeSurfaceContainer: Boolean,
    isYouPlayer: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        player.selection.chunked(2).forEachIndexed { index, chunk ->
            if (index == 1) {
                Spacer(Modifier.height(12.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .background(
                        color = if (useAlternativeSurfaceContainer) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(50.dp)
                    )
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .width(400.dp)
            ) {
                if (isYouPlayer) {
                    Text(if (index == 0) "Lead" else "Back", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(16.dp))
                }
                chunk.forEachIndexed { cIndex, pokemon ->
                    if (cIndex == 1) {
                        Spacer(Modifier.width(16.dp))
                    }
                    val teraType = player.terastallization?.takeIf { it.pokemon.matches(pokemon) }?.type
                    SelectedPokemon(
                        modifier = Modifier.widthIn(max = 175.dp),
                        pokemon = pokemon,
                        teraType = teraType,
                        pokemonImageService = pokemonImageService,
                        isYouPlayer = isYouPlayer
                    )
                }
                if (!isYouPlayer) {
                    Spacer(Modifier.width(16.dp))
                    Text(if (index == 0) "Lead" else "Back", style = MaterialTheme.typography.titleLarge)
                }
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
            Text(if (!viewModel.useCase.hasFilteredReplays) "No replays were found" else "No replays matched the filters")
        }

        LinearProgressBarIfEnabled(viewModel.isLoading, modifier = Modifier
            .align(Alignment.TopStart))
    }
}

@Composable
private fun VsText(currentPlayer: Player, opponentPlayer: Player, gameOutput: GameOutput) {
    val text =
        if (gameOutput != GameOutput.UNKNOWN) "VS ${opponentPlayer.name.value}"
        else "${currentPlayer.name.value} VS ${opponentPlayer.name.value}"
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 8.dp)
    )
}