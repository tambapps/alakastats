package com.tambapps.pokemon.alakastats

import androidx.compose.runtime.Composable
import com.tambapps.pokemon.alakastats.di.appModules
import org.koin.compose.KoinApplication

@Composable
fun IosApp() {
    KoinApplication(application = {
        modules(appModules)
    }) {
        App()
    }
}