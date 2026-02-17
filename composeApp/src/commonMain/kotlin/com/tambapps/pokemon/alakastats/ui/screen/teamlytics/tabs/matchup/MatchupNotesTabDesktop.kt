package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.matchup

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact


@Composable
fun MatchupNotesTabDesktop(viewModel: MatchupNotesViewModel, scrollState: LazyListState) {
    val matchupNotes = viewModel.matchupNotes

    LazyColumn(Modifier.padding(horizontal = 16.dp), state = scrollState) {
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
                    gamePlan.composition
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { composition ->
                            Composition(
                                composition, viewModel.pokemonImageService,
                                pokemonSize = 120.dp,
                                facingDirection = FacingDirection.RIGHT,
                                Modifier.padding(end = 8.dp).weight(4f)
                            )
                        }
                    Text(
                        gamePlan.description,
                        style = if (LocalIsCompact.current) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(6f))
                }
                if (gamePlan.exampleReplays.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    ExampleReplays(viewModel, gamePlan)
                }
                Spacer(Modifier.height(32.dp))

            }
            Spacer(Modifier.height(8.dp))
        }
    }
    if (showDeleteDialog) {
        DeleteMatchupDialog(viewModel, matchupNotes, onDismiss = { showDeleteDialog = false })
    }
}
