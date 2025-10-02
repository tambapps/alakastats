package com.tambapps.pokemon.alakastats.di

import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.service.PokemonLocalUrlImageService
import com.tambapps.pokemon.alakastats.util.getCurrentBaseUrl
import org.koin.dsl.module

val wasmModule = module {
    single<PokemonImageService> { PokemonLocalUrlImageService(get(), getCurrentBaseUrl()) }
}