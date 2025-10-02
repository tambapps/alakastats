package com.tambapps.pokemon.alakastats.di

import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.service.PokemonUrlMappingImageService
import org.koin.dsl.module

val androidModule = module {
    single<PokemonImageService> { PokemonUrlMappingImageService(get()) }
}