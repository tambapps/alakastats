package com.tambapps.pokemon.alakastats.di

import androidx.compose.runtime.Composable

@Composable
expect fun InitializeKoin(content: @Composable () -> Unit)