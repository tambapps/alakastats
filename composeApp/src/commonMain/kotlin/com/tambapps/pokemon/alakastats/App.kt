package com.tambapps.pokemon.alakastats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.tambapps.pokemon.alakastats.infrastructure.sync.TeamSyncService
import com.tambapps.pokemon.alakastats.ui.screen.cloudsync.SyncMergeNotifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import com.tambapps.pokemon.alakastats.ui.SnackBarContext
import com.tambapps.pokemon.alakastats.ui.screen.home.HomeScreen
import com.tambapps.pokemon.alakastats.ui.service.LocalPokemonImageService
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.theme.AppTheme
import com.tambapps.pokemon.alakastats.ui.theme.ProvideIsCompact
import org.koin.compose.koinInject

/**
 * @param startScreen the first screen, e.g. the damage calc alone on the web app's damage-calc page
 */
@Composable
fun App(startScreen: Screen = HomeScreen) {
    val syncService = koinInject<TeamSyncService>()
    LaunchedEffect(Unit) {
        // syncs only if the user connected a cloud storage
        syncService.start()
    }
    LifecycleResumeEffect(Unit) {
        // back to the app: changes may have been made on another device
        syncService.syncIfStale()
        onPauseOrDispose {}
    }
    AppTheme {
        ProvideIsCompact {
            SnackBarContext {
                SyncMergeNotifier(syncService)
                CompositionLocalProvider(LocalPokemonImageService provides koinInject<PokemonImageService>()) {
                    Navigator(startScreen)
                }
            }
        }
    }
}