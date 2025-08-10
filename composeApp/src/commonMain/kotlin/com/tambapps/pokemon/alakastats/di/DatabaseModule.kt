package com.tambapps.pokemon.alakastats.di

import com.tambapps.pokemon.alakastats.database.SdReplayRepository
import com.tambapps.pokemon.alakastats.database.DatabaseProvider
import org.koin.dsl.module

val databaseModule = module {
    // DriverFactory is provided by platform-specific modules
    
    // Database provider that creates database lazily
    single<DatabaseProvider> { DatabaseProvider(get()) }
    
    // Repository for SdReplay operations
    single<SdReplayRepository> { SdReplayRepository(get()) }
}