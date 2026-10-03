package com.tambapps.pokemon.alakastats

import androidx.compose.runtime.Composable
import com.tambapps.pokemon.alakastats.di.appModules
import com.tambapps.pokemon.alakastats.di.wasmModule
import com.tambapps.pokemon.alakastats.ui.screen.damagecalc.StandaloneDamageCalcScreen
import com.tambapps.pokemon.alakastats.ui.screen.home.HomeScreen
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
        App(startScreen = if (isDamageCalcPage) StandaloneDamageCalcScreen else HomeScreen)
    }
}