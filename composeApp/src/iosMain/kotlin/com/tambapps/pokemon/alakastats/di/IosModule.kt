package com.tambapps.pokemon.alakastats.di

import com.tambapps.pokemon.alakastats.ui.service.GhPagesImageService
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import org.koin.dsl.module

val iosModule = module {
    single<PokemonImageService> { GhPagesImageService(get()) }
    /* HttpClient to make calls to graphql.pokeApi work on simulator.
    Don't forget the allowOverride(true) in IosApp.kt
    single<HttpClient> {
        HttpClient(Darwin) {
            install(ContentNegotiation) {
                json(get<Json>())
            }
            engine {
                val delegate = KtorNSURLSessionDelegate()
                val session = NSURLSession.sessionWithConfiguration(
                    NSURLSessionConfiguration.ephemeralSessionConfiguration(),
                    delegate,
                    delegateQueue = null
                )
                usePreconfiguredSession(session, delegate)
            }
        }
    }

     */
}
