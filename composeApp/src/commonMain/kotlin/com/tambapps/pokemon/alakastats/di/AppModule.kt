package com.tambapps.pokemon.alakastats.di

import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import com.tambapps.pokemon.alakastats.ui.screen.home.HomeViewModel
import org.koin.dsl.module

val appModule = module {
    single<PokepasteParser> { PokepasteParser() }
    factory { HomeViewModel() }
}