package com.tambapps.pokemon.alakastats.di

import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import com.tambapps.pokemon.alakastats.ui.screen.home.HomeViewModel
import com.tambapps.pokemon.alakastats.ui.screen.editteam.EditTeamViewModel
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.domain.transformer.TeamlyticsTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.ReplayAnalyticsTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.SdReplayTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.PlayerTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.TeamPreviewTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.TeamPreviewPokemonTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.OpenTeamSheetTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.OtsPokemonTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.TeamlyticsPreviewTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.TerastallizationTransformer
import com.tambapps.pokemon.alakastats.domain.usecase.CreateTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.TeamlyticsHomeUseCase
import com.tambapps.pokemon.alakastats.infrastructure.repository.KStoreTeamlyticsRepository
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.KStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.createTeamlyticsKStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.createTeamlyticsPreviewKStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsPreviewEntity
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid
import org.koin.core.qualifier.named
import org.koin.dsl.module
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

private val appModule = module {
    single { Json { ignoreUnknownKeys = true } }
    single<HttpClient> {
        HttpClient {
            install(ContentNegotiation) {
                json(get<Json>())
            }
        }
    }
    single<PokemonImageService> { PokemonImageService(get()) }
    single<PokepasteParser> { PokepasteParser() }
    // need to name them as they have both the same signature after generic type erasure
    single<KStorage<Uuid, TeamlyticsEntity>>(named("teamsStorage")) { createTeamlyticsKStorage() }
    single<KStorage<Uuid, TeamlyticsPreviewEntity>>(named("previewsStorage")) { createTeamlyticsPreviewKStorage() }
    single<TeamlyticsRepository> { 
        KStoreTeamlyticsRepository(
            teamsStorage = get(named("teamsStorage")), 
            previewsStorage = get(named("previewsStorage")), 
            teamlyticsTransformer = get(), 
            previewTransformer = get()
        ) 
    }
    single<CreateTeamlyticsUseCase> { CreateTeamlyticsUseCase(get()) }
    single<TeamlyticsHomeUseCase> { TeamlyticsHomeUseCase(get()) }
    factory { HomeViewModel(get(), get()) }
    factory { EditTeamViewModel(get(), get(), get()) }
}

private val transformerModule = module {
    single { TerastallizationTransformer() }
    single { TeamPreviewPokemonTransformer() }
    single { OtsPokemonTransformer() }
    single { TeamPreviewTransformer(get()) }
    single { OpenTeamSheetTransformer(get()) }
    single { PlayerTransformer(get(), get(), get()) }
    single { SdReplayTransformer(get()) }
    single { ReplayAnalyticsTransformer(get()) }
    single { TeamlyticsTransformer(get(), get()) }
    single { TeamlyticsPreviewTransformer() }
}

val appModules = listOf(appModule, transformerModule)