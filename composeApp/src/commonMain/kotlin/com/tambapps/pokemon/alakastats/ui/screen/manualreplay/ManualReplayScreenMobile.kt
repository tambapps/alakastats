package com.tambapps.pokemon.alakastats.ui.screen.manualreplay

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.composables.verticalPokemonSpace
import com.tambapps.pokemon.alakastats.ui.theme.teamlyticsTabPaddingBottom

@Composable
internal fun <T> ManualPokemonGridMobile(
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
        pokemonStates.forEach { pokemonState ->
            card(pokemonState, Modifier.fillMaxWidth())
            Spacer(Modifier.height(verticalPokemonSpace))
        }
        addCard?.invoke(Modifier.fillMaxWidth())
        Spacer(Modifier.height(teamlyticsTabPaddingBottom))
    }
}
