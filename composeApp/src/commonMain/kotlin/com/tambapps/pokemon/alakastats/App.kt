package com.tambapps.pokemon.alakastats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import cafe.adriel.voyager.navigator.Navigator
import com.tambapps.pokemon.alakastats.ui.SnackBarContext
import com.tambapps.pokemon.alakastats.ui.screen.home.HomeScreen
import com.tambapps.pokemon.alakastats.ui.service.LocalPokemonImageService
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.theme.AppTheme
import com.tambapps.pokemon.alakastats.ui.theme.ProvideIsCompact
import org.koin.compose.koinInject

@Composable
fun App() {
    AppTheme {
        ProvideIsCompact {
            SnackBarContext {
                CompositionLocalProvider(LocalPokemonImageService provides koinInject<PokemonImageService>()) {
                    Navigator(HomeScreen)
                }
            }
        }
    }
}