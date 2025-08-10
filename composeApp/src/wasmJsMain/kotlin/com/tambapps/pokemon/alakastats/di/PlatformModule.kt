package com.tambapps.pokemon.alakastats.di

import com.tambapps.pokemon.alakastats.database.DriverFactory
import org.koin.dsl.module

actual val platformModule = module {
    single<DriverFactory> { DriverFactory() }
}