package com.tambapps.pokemon.alakastats

import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import cafe.adriel.voyager.navigator.Navigator
import com.tambapps.pokemon.alakastats.di.appModules
import com.tambapps.pokemon.alakastats.ui.screen.home.HomeScreen
import com.tambapps.pokemon.alakastats.ui.theme.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplication

@Composable
@Preview
fun App() {
    KoinApplication(application = {
        modules(appModules)
    }) {
        val isDarkTheme = isSystemInDarkTheme()

        AppTheme(darkTheme = isDarkTheme) {
            Navigator(HomeScreen)
        }
    }
}
