package com.tambapps.pokemon.alakastats.ui.screen.showdownreplays

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.close
import alakastats.composeapp.generated.resources.delete
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.composables.PokemonTeamPreview
import org.jetbrains.compose.resources.painterResource

/**
 * In place of the results' header while selecting: how many are selected, selecting them all, and the actions on them
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SelectionBar(viewModel: ShowdownReplaysViewModel, modifier: Modifier = Modifier) {
    val count = viewModel.selectedIds.size
    val progress = viewModel.addToTeamProgress
    Column(modifier) {
        // on the right: the navigation's menu button is over the top left corner when the bar sticks to the top
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            itemVerticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::stopSelection, enabled = progress == null) {
                    Icon(painterResource(Res.drawable.close), contentDescription = "Stop selecting")
                }
                Text(
                    if (count == 1) "1 selected" else "$count selected",
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(onClick = viewModel::toggleSelectAll, enabled = progress == null) {
                    Text(if (viewModel.areAllShownSelected) "Unselect all" else "Select all")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::askSelectionDeletion, enabled = count > 0 && progress == null && !viewModel.isBusy) {
                    Icon(painterResource(Res.drawable.delete), contentDescription = "Delete the selected replays")
                }
                FilledTonalButton(onClick = viewModel::askTeam, enabled = count > 0 && progress == null) {
                    Text("Add to team")
                }
            }
        }
        progress?.let {
            LinearProgressIndicator(
                progress = { if (it.total == 0) 0f else it.fetched.toFloat() / it.total },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Adding to ${it.teamName}… ${it.fetched} / ${it.total}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/**
 * The teams to add the selected replays to
 */
@Composable
internal fun TeamPickerDialog(viewModel: ShowdownReplaysViewModel) {
    val teams = viewModel.teamsToPick ?: return
    val snackBar = LocalSnackBar.current
    val count = viewModel.selectedIds.size
    AlertDialog(
        onDismissRequest = viewModel::dismissTeamPicker,
        title = { Text(if (count == 1) "Add 1 replay to a team" else "Add $count replays to a team") },
        text = {
            if (teams.isEmpty()) {
                Text("No team yet: create one in Teams first")
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(teams, key = { it.id.toString() }) { team ->
                        TeamChoice(team = team, onClick = { viewModel.addSelectionToTeam(team, snackBar) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = viewModel::dismissTeamPicker) { Text("Cancel") } },
    )
}

@Composable
private fun TeamChoice(team: TeamlyticsPreview, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Text(team.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        PokemonTeamPreview(team.pokemons, team.items, fillWidth = true)
    }
}
