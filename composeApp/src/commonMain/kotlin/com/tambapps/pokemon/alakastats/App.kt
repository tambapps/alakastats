package com.tambapps.pokemon.alakastats

import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import cafe.adriel.voyager.navigator.Navigator
import com.tambapps.pokemon.alakastats.di.InitializeKoin
import com.tambapps.pokemon.alakastats.ui.screen.home.HomeScreen
import com.tambapps.pokemon.alakastats.ui.theme.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    InitializeKoin {
        val isDarkTheme = isSystemInDarkTheme()

        AppTheme(darkTheme = isDarkTheme) {
            Navigator(HomeScreen)
        }
    }
}
