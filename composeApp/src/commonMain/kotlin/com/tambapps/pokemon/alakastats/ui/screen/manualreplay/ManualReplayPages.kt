package com.tambapps.pokemon.alakastats.ui.screen.manualreplay

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.add
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.composables.LOOSE_COLOR
import com.tambapps.pokemon.alakastats.ui.composables.MegaSwitch
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.WIN_COLOR
import com.tambapps.pokemon.alakastats.ui.composables.cardGradientColors
import com.tambapps.pokemon.alakastats.ui.service.FacingDirection
import com.tambapps.pokemon.alakastats.ui.service.PokemonSprite
import com.tambapps.pokemon.alakastats.ui.theme.defaultIconColor
import com.tambapps.pokemon.alakastats.ui.theme.teamlyticsTabPaddingBottom
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun YouPage(viewModel: ManualReplayViewModel) {
    ManualPokemonGrid(
        pokemonStates = viewModel.youPokemonStates,
        header = {
            Column {
                GameOutcomeChoice(viewModel)
                Spacer(Modifier.height(16.dp))
                PageHeader(
                    "Select the ${MAX_SELECTION} pokemons you brought. The first ${LEAD_SIZE} selected ones are your lead."
                )
            }
        }
    ) { pokemonState, modifier ->
        // the user's pokemons are already known, they only have to be selected
        SelectableManualPokemonCard(viewModel, pokemonState, modifier)
    }
}

@Composable
internal fun OpponentPage(viewModel: ManualReplayViewModel) {
    ManualPokemonGrid(
        pokemonStates = viewModel.opponentPokemonStates,
        addCard = if (viewModel.canAddOpponentPokemon) ({ modifier ->
            AddPokemonCard(viewModel, modifier)
        }) else null
    ) { pokemonState, modifier ->
        OpponentPokemonCard(viewModel, pokemonState, modifier)
    }
}

@Composable
internal fun NotesPage(viewModel: ManualReplayViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        PageHeader("Write down anything worth remembering about this game.")
        OutlinedTextField(
            value = viewModel.notes,
            onValueChange = viewModel::updateNotes,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = teamlyticsTabPaddingBottom),
            label = { Text("Notes") },
        )
    }
}

@Composable
private fun GameOutcomeChoice(viewModel: ManualReplayViewModel) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Did you win?", style = MaterialTheme.typography.titleMedium)
        GameOutcomeChip(viewModel, "Won", won = true, color = WIN_COLOR)
        GameOutcomeChip(viewModel, "Lost", won = false, color = LOOSE_COLOR)
    }
}

@Composable
private fun GameOutcomeChip(
    viewModel: ManualReplayViewModel,
    label: String,
    won: Boolean,
    color: Color
) {
    FilterChip(
        selected = viewModel.won == won,
        onClick = { viewModel.updateWon(won) },
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = color)
    )
}

@Composable
private fun PageHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

@Composable
private fun OpponentPokemonCard(
    viewModel: ManualReplayViewModel,
    pokemonState: OpponentPokemonState,
    modifier: Modifier = Modifier
) {
    SelectableManualPokemonCard(
        viewModel = viewModel,
        pokemonState = pokemonState,
        modifier = modifier,
        trailingContent = {
            TextButton(onClick = { viewModel.removeOpponentPokemon(pokemonState) }) {
                Text("×", style = MaterialTheme.typography.titleLarge)
            }
        }
    )
}

// both players tell which pokemons they brought the same way
@Composable
private fun SelectableManualPokemonCard(
    viewModel: ManualReplayViewModel,
    pokemonState: ManualPokemonState,
    modifier: Modifier = Modifier,
    trailingContent: @Composable () -> Unit = {}
) {
    val selectionIndex = viewModel.selectionIndexOf(pokemonState)
    val selected = selectionIndex >= 0

    MyCard(
        modifier = modifier,
        onClick = { viewModel.toggleSelected(pokemonState) },
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        gradientBackgroundColors = cardGradientColors,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // it can only have mega evolved in a battle it was brought to
            ManualPokemonCardHeader(viewModel, pokemonState, selected, trailingContent)

            Spacer(Modifier.height(8.dp))

            Text(
                text = when {
                    !selected -> "Not selected"
                    selectionIndex < LEAD_SIZE -> "Lead"
                    else -> "Back"
                },
                style = MaterialTheme.typography.titleSmall,
                color =
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ManualPokemonCardHeader(
    viewModel: ManualReplayViewModel,
    pokemonState: ManualPokemonState,
    megaEnabled: Boolean = true,
    trailingContent: @Composable () -> Unit = {}
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PokemonSprite(
            pokemonState.displayedName,
            Modifier.size(56.dp),
            facingDirection = FacingDirection.RIGHT
        )
        Spacer(Modifier.width(8.dp))
        AnimatedContent(
            targetState = pokemonState.displayedName.pretty,
            transitionSpec = { (fadeIn() + slideInVertically { it }) togetherWith (fadeOut() + slideOutVertically { -it }) },
            modifier = Modifier.weight(1f)
        ) { name ->
            Text(name, style = MaterialTheme.typography.titleMedium)
        }
        if (pokemonState.canMega) {
            MegaSwitch(
                megaSelected = pokemonState.megaSelected,
                enabled = megaEnabled,
                onCheckedChange = { viewModel.updateMegaSelected(pokemonState, it) }
            )
        }
        trailingContent()
    }
}

// takes the place of the next pokemon card, so that adding one stays where the eye already is
@Composable
private fun AddPokemonCard(viewModel: ManualReplayViewModel, modifier: Modifier = Modifier) {
    MyCard(
        modifier = modifier,
        onClick = { viewModel.showSelectPokemonDialog() },
        gradientBackgroundColors = cardGradientColors,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(Res.drawable.add),
                contentDescription = "Add Pokemon",
                tint = MaterialTheme.colorScheme.defaultIconColor,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text("Add Pokemon", style = MaterialTheme.typography.titleMedium)
        }
    }
}
