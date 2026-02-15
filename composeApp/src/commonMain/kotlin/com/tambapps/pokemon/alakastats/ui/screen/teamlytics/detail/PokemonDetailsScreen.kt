package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import org.koin.core.parameter.parametersOf
import kotlin.uuid.Uuid

data class PokemonDetailsScreen(
    val teamId: Uuid,
    val pokemonNameStr: String
): Screen {

    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<PokemonDetailViewModel> { parametersOf(teamId, PokemonName(pokemonNameStr)) }

        val isCompact = LocalIsCompact.current
        Column(
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .then(
                    if (isCompact) Modifier.safeContentPadding().padding(horizontal = 4.dp)
                    else Modifier.padding(all = 32.dp)
                )
                .verticalScroll(rememberScrollState()),
        ) {

        }
    }

}