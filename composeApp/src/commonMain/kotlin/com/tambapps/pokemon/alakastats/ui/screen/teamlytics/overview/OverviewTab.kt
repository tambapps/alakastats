package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.overview

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.computeWinRate
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact

@Composable
fun OverviewTab(viewModel: OverviewViewModel) {
    val isCompact = LocalIsCompact.current
    if (isCompact) {
        OverviewTabMobile(viewModel)
    } else {
        OverviewTabDesktop(viewModel)
    }
}

@Composable
internal fun TeamName(team: Teamlytics) {
    Text(
        text = team.name,
        style = MaterialTheme.typography.displayLarge,
        fontWeight = FontWeight.Bold
    )
}

@Composable
internal fun Header(team: Teamlytics) {
    val replaysCount = remember { team.replays.size }
    val textStyle = MaterialTheme.typography.titleLarge
    if (replaysCount == 0) {
        Text(
            "${team.replays.size} replays",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = textStyle
        )
        return
    }
    Row(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        val space = Modifier.width(32.dp)
        Spacer(space)
        Text("$replaysCount replays", style = textStyle)
        Spacer(Modifier.weight(1f))
        val winRate = remember { team.computeWinRate() }
        Text("$winRate% winrate", style = textStyle)
        Spacer(space)
    }
}

@Composable
internal fun PokePasteTitle() {
    Text(
        text = "PokePaste",
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold
    )
}