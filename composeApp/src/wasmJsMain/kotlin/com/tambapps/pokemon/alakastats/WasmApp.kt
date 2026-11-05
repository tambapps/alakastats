package com.tambapps.pokemon.alakastats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tambapps.pokemon.alakastats.di.appModules
import com.tambapps.pokemon.alakastats.di.wasmModule
import com.tambapps.pokemon.alakastats.ui.navigation.AppRoute
import com.tambapps.pokemon.alakastats.ui.navigation.BrowserHistory
import com.tambapps.pokemon.alakastats.ui.navigation.BrowserNavigationSync
import com.tambapps.pokemon.alakastats.ui.navigation.WebRoutes
import com.tambapps.pokemon.alakastats.ui.sitemove.isOldWebAddress
import kotlinx.browser.window
import org.koin.compose.KoinApplication
import org.koin.core.logger.Level
import org.koin.dsl.koinConfiguration

// the page with the damage calc alone (damage-calc/index.html)
private const val DAMAGE_CALC_PATH = "/damage-calc"

@Composable
fun WasmApp() {
    KoinApplication(
        configuration = koinConfiguration {
            modules(appModules + wasmModule)
        },
        // this overload sets up a print logger, which we don't want
        logLevel = Level.NONE
    ) {
        val isDamageCalcPage = window.location.pathname.removeSuffix("/").removeSuffix("/index.html")
            .endsWith(DAMAGE_CALC_PATH)
        when {
            isDamageCalcPage -> App(startRoutes = listOf(AppRoute.StandaloneDamageCalc))
            // an old address is only there for the users to move: it has no URLs for the screens
            isOldWebAddress(platform.webHost) -> App()
            else -> {
                val history = remember { BrowserHistory() }
                App(
                    startRoutes = remember { WebRoutes.backStackOf(history.path) },
                    backStackEffect = { BrowserNavigationSync(it, history) },
                )
            }
        }
    }
}