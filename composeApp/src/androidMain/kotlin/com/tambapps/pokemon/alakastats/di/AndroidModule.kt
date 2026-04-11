package com.tambapps.pokemon.alakastats.di

import com.tambapps.pokemon.alakastats.ui.service.GhPagesImageService
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import org.koin.dsl.module

val androidModule = module {
    single<PokemonImageService> { GhPagesImageService(get()) }
}