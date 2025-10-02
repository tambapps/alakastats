package com.tambapps.pokemon.alakastats.di

import com.tambapps.pokemon.alakastats.ui.service.IPokemonImageService
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import org.koin.dsl.module

val androidModule = module {
    single<IPokemonImageService> { PokemonImageService(get()) }
}