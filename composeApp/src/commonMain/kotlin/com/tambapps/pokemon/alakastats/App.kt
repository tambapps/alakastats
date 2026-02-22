package com.tambapps.pokemon.alakastats

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import com.tambapps.pokemon.alakastats.ui.SnackBarContext
import com.tambapps.pokemon.alakastats.ui.screen.home.HomeScreen
import com.tambapps.pokemon.alakastats.ui.theme.AppTheme
import com.tambapps.pokemon.alakastats.ui.theme.ProvideIsCompact

@Composable
fun App() {
    AppTheme {
        ProvideIsCompact {
            SnackBarContext {
                Navigator(HomeScreen)
            }
        }
    }
}