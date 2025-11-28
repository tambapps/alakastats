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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.MatchupNotes
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
    MyCard(
        modifier = modifier,
        gradientBackgroundColors = cardGradientColors,
    ) {
        Column(Modifier.padding(all = 8.dp).fillMaxWidth()) {
            Row {
                Text(
                    text = "VS " + matchupNotes.name,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = {  },
                    colors = IconButtonDefaults.iconButtonColors().copy(contentColor = MaterialTheme.typography.headlineLarge.color)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.more_horiz),
                        contentDescription = "More"
                    )
                }
            }
            matchupNotes.pokePaste?.let { pokePaste -> PokemonTeamPreview(viewModel.pokemonImageService, pokePaste.pokemons.map { it.name }, fillWidth = true) }
            Spacer(Modifier.height(8.dp))
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
}