package com.tambapps.pokemon.alakastats.di

import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import com.tambapps.pokemon.alakastats.ui.screen.home.HomeViewModel
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val appModule = module {
    single { Json { ignoreUnknownKeys = true } }
    single<PokemonImageService> { PokemonImageService(get()) }
    single<PokepasteParser> { PokepasteParser() }
    factory { HomeViewModel(get()) }
}