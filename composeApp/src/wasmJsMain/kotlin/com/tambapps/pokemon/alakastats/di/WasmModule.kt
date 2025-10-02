package com.tambapps.pokemon.alakastats.di

import com.tambapps.pokemon.alakastats.ui.service.IPokemonImageService
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.service.PokemonUrlImageService
import com.tambapps.pokemon.alakastats.util.getCurrentBaseUrl
import org.koin.dsl.module

val wasmModule = module {
    single<IPokemonImageService> { PokemonUrlImageService(get(), getCurrentBaseUrl()) }
}