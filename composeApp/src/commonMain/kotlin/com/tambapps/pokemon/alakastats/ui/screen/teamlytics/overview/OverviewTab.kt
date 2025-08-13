package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.overview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.TeamlyticsViewModel


@Composable
fun OverviewTab(viewModel: TeamlyticsViewModel) {
    val team = viewModel.requireTeam()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = team.name,
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Team overview content will go here",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
