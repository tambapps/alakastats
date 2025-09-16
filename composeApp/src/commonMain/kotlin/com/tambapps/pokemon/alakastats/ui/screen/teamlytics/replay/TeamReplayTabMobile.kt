package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.replay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics

@Composable
internal fun TeamReplayTabMobile(viewModel: TeamReplayViewModel) {
    val team = viewModel.team

    if (team.replays.isEmpty()) {
        NoReplaysMobile(viewModel)
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            AddReplayButton(viewModel)
        }
        val replays = team.replays
        itemsIndexed(replays) { index, replay ->
            MobileReplay(viewModel, team, replay)
            if (index > 0 && index < replays.size - 1) {
                HorizontalDivider(modifier = Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun MobileReplay(viewModel: TeamReplayViewModel, team: Teamlytics, replay: ReplayAnalytics) {
    val (currentPlayer, opponentPlayer) = team.getPlayers(replay)
    Text("VS ${opponentPlayer.name}", style = MaterialTheme.typography.titleMedium)
}

@Composable
internal fun NoReplaysMobile(viewModel: TeamReplayViewModel) {
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
            ProgressBar(
                modifier = Modifier
                    .align(Alignment.BottomStart)
            )
        }
    }
}

@Composable
private fun ProgressBar(modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)  // thickness
    )
}