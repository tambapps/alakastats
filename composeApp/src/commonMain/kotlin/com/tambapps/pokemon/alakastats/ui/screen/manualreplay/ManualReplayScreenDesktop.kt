package com.tambapps.pokemon.alakastats.ui.screen.manualreplay

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.composables.verticalPokemonSpace
import com.tambapps.pokemon.alakastats.ui.theme.teamlyticsTabPaddingBottom

private const val POKEMONS_PER_ROW = 3

@Composable
internal fun <T> ManualPokemonGridDesktop(
    pokemonStates: List<T>,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    addCard: @Composable ((Modifier) -> Unit)?,
    card: @Composable (T, Modifier) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        header()
        // the null cell is the add card, taking the next free slot of the grid
        val cells: List<T?> = buildList {
            addAll(pokemonStates)
            if (addCard != null) {
                add(null)
            }
        }
        for (cellBlock in cells.chunked(POKEMONS_PER_ROW)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = verticalPokemonSpace)
            ) {
                cellBlock.forEachIndexed { index, pokemonState ->
                    if (index > 0) {
                        Spacer(Modifier.width(16.dp))
                    }
                    if (pokemonState != null) {
                        card(pokemonState, Modifier.weight(1f))
                    } else {
                        addCard?.invoke(Modifier.weight(1f))
                    }
                }
                // keep cards of incomplete rows the same width as the ones of full rows
                repeat(POKEMONS_PER_ROW - cellBlock.size) {
                    Spacer(Modifier.width(16.dp))
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(teamlyticsTabPaddingBottom))
    }
}
