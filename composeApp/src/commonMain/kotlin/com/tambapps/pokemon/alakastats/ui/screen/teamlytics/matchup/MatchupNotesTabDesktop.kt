package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.matchup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.MatchupNotes
import com.tambapps.pokemon.alakastats.ui.service.FacingDirection


@Composable
fun MatchupNotesTabDesktop(viewModel: MatchupNotesViewModel) {
    val matchupNotes = viewModel.matchupNotes

    LazyColumn(Modifier.padding(horizontal = 16.dp)) {
        item { Spacer(Modifier.height(32.dp)) }
        items(matchupNotes) {
            MatchNotesDesktop(viewModel, it, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(32.dp))
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}


@Composable
private fun MatchNotesDesktop(
    viewModel: MatchupNotesViewModel,
    matchupNotes: MatchupNotes,
    modifier: Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    MatchNotes(viewModel = viewModel, matchupNotes = matchupNotes, modifier = modifier) {
        Column(Modifier.padding(all = 8.dp).fillMaxWidth()) {
            Spacer(Modifier.height(16.dp))
            matchupNotes.gamePlans.forEachIndexed { index, gamePlan ->
                Text(
                    text = "Game Plan ${index + 1}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(gamePlan.description, style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(6f))
                    gamePlan.composition
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { composition ->
                            Composition(
                                composition, viewModel.pokemonImageService,
                                pokemonSize = 120.dp,
                                facingDirection = FacingDirection.LEFT,
                                Modifier.padding(end = 8.dp).weight(4f)
                            )
                        }
                }
                Spacer(Modifier.height(16.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    if (showDeleteDialog) {
        DeleteMatchupDialog(viewModel, matchupNotes, onDismiss = { showDeleteDialog = false })
    }
}
