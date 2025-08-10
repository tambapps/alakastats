package com.tambapps.pokemon.alakastats.di

import androidx.compose.runtime.Composable
import org.koin.compose.KoinApplication

@Composable
actual fun InitializeKoin(content: @Composable () -> Unit) {
    KoinApplication(application = {
        modules(allModules)
    }) {
        content()
    }
}