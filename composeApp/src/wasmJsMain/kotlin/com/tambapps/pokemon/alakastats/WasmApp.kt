package com.tambapps.pokemon.alakastats

import androidx.compose.runtime.Composable
import com.tambapps.pokemon.alakastats.di.appModules
import com.tambapps.pokemon.alakastats.di.wasmModule
import org.koin.compose.KoinApplication
import org.koin.core.logger.Level
import org.koin.dsl.koinConfiguration

@Composable
fun WasmApp() {
    KoinApplication(
        configuration = koinConfiguration {
            modules(appModules + wasmModule)
        },
        // this overload sets up a print logger, which we don't want
        logLevel = Level.NONE
    ) {
        App()
    }
}