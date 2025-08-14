package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.notes

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact

@Composable
fun TeamNotesTab(viewModel: TeamNotesViewModel) {
    val isCompact = LocalIsCompact.current
    if (isCompact) {
        TeamNotesTabMobile(viewModel)
    } else {
        TeamNotesTabDesktop(viewModel)
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
