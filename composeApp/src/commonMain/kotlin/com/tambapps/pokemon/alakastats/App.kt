package com.tambapps.pokemon.alakastats

import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import cafe.adriel.voyager.navigator.Navigator
import com.tambapps.pokemon.alakastats.ui.SnackBarContext
import com.tambapps.pokemon.alakastats.ui.screen.home.HomeScreen
import com.tambapps.pokemon.alakastats.ui.theme.AppTheme
import com.tambapps.pokemon.alakastats.ui.theme.ProvideIsCompact
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    val isDarkTheme = isSystemInDarkTheme()

    AppTheme(darkTheme = isDarkTheme) {
        ProvideIsCompact {
            SnackBarContext {
                Navigator(HomeScreen)
            }
        }
    }
}