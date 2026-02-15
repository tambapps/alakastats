package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.SnackBar
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
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            when (val state = viewModel.state) {
                is TeamPokemonStateState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp).align(Alignment.Center)
                    )
                }
                is TeamPokemonStateState.Error -> {
                    val navigator = LocalNavigator.currentOrThrow
                    val snackBar = LocalSnackBar.current
                    LaunchedEffect(Unit) {
                        snackBar.show("Error: ${state.error.message}", SnackBar.Severity.ERROR)
                        navigator.pop()
                    }
                }
                is TeamPokemonStateState.Loaded -> {
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(durationMillis = 1000))
                    ) {
                        PokemonDetails(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun PokemonDetails(viewModel: PokemonDetailViewModel) {

}