package com.tambapps.pokemon.alakastats.di

import androidx.compose.runtime.Composable

@Composable
actual fun InitializeKoin(content: @Composable () -> Unit) {
    // Koin is already initialized in AlakastatsApplication
    content()
}