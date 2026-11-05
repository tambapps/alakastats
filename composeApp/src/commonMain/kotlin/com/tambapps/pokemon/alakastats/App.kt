package com.tambapps.pokemon.alakastats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.tambapps.pokemon.alakastats.infrastructure.sync.TeamSyncService
import com.tambapps.pokemon.alakastats.ui.screen.cloudsync.SyncMergeNotifier
import com.tambapps.pokemon.alakastats.ui.SnackBarContext
import com.tambapps.pokemon.alakastats.ui.navigation.AppNavDisplay
import com.tambapps.pokemon.alakastats.ui.navigation.AppRoute
import com.tambapps.pokemon.alakastats.ui.service.LocalPokemonImageService
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.sitemove.SiteMoveBanner
import com.tambapps.pokemon.alakastats.ui.theme.AppTheme
import com.tambapps.pokemon.alakastats.ui.theme.ProvideIsCompact
import org.koin.compose.koinInject

/**
 * @param startRoutes the screens at start, the last one being shown, e.g. the damage calc alone on the web app's
 * damage-calc page
 * @param backStackEffect what follows the back stack, e.g. the web app's URL
 */
@Composable
fun App(
    startRoutes: List<AppRoute> = listOf(AppRoute.Home),
    backStackEffect: @Composable (NavBackStack<NavKey>) -> Unit = {},
) {
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
                    SiteMoveBanner {
                        AppNavDisplay(startRoutes, backStackEffect)
                    }
                }
            }
        }
    }
}