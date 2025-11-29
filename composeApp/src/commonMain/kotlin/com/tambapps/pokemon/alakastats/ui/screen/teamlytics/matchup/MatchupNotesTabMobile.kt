package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.matchup

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.more_horiz
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.MatchupNotes
import com.tambapps.pokemon.alakastats.ui.composables.ExpansionTile
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.PokemonTeamPreview
import com.tambapps.pokemon.alakastats.ui.composables.cardGradientColors
import org.jetbrains.compose.resources.painterResource


@Composable
fun MatchupNotesTabMobile(viewModel: MatchupNotesViewModel) {
    val matchupNotes = viewModel.matchupNotes

    LazyColumn(Modifier.padding(horizontal = 16.dp)) {
        item { Spacer(Modifier.height(32.dp)) }

        items(matchupNotes) {
            MatchNotesMobile(viewModel, it, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(32.dp))
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun MatchNotesMobile(viewModel: MatchupNotesViewModel, matchupNotes: MatchupNotes, modifier: Modifier) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    ExpansionTile(
        gradientBackgroundColors = cardGradientColors,
        title = {
            Text(
                text = matchupNotes.name,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        subtitle = {
            matchupNotes.pokePaste?.let { pokePaste -> PokemonTeamPreview(viewModel.pokemonImageService, pokePaste.pokemons.map { it.name }, fillWidth = true) }
        },
        menu = { isMenuExpandedState ->
            DropdownMenu(
                expanded = isMenuExpandedState.value,
                onDismissRequest = { isMenuExpandedState.value = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = { viewModel.editMatchupMode = EditMatchup(matchupNotes) }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { showDeleteDialog = true }
                )
            }
        }
    ) {
        Column(Modifier.padding(all = 8.dp).fillMaxWidth()) {
            matchupNotes.gamePlans.forEachIndexed { index, gamePlan ->
                Text(
                    text = "Game Plan ${index + 1}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                gamePlan.composition
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { composition -> Composition(composition, viewModel.pokemonImageService) }

                Text(gamePlan.description, style = MaterialTheme.typography.bodyLarge)

                Spacer(Modifier.height(16.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    if (showDeleteDialog) {
        DeleteMatchupDialog(viewModel, matchupNotes, onDismiss = { showDeleteDialog = false })
    }
}

