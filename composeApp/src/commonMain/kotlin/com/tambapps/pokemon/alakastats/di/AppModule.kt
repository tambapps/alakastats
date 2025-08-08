package com.tambapps.pokemon.alakastats.di

import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import org.koin.dsl.module

val appModule = module {
    single<PokepasteParser> { PokepasteParser() }
}