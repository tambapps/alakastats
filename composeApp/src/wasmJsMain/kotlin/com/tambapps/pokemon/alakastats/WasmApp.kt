package com.tambapps.pokemon.alakastats

import androidx.compose.runtime.Composable
import com.tambapps.pokemon.alakastats.di.appModules
import com.tambapps.pokemon.alakastats.di.wasmModule
import org.koin.compose.KoinApplication

@Composable
fun WasmApp() {
    KoinApplication(application = {
        modules(appModules + wasmModule)
    }) {
        App()
    }
}