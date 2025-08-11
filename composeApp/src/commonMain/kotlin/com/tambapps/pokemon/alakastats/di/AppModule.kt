package com.tambapps.pokemon.alakastats.di

import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import com.tambapps.pokemon.alakastats.ui.screen.home.HomeViewModel
import com.tambapps.pokemon.alakastats.ui.screen.createteam.CreateTeamViewModel
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.domain.transformer.TeamlyticsTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.ReplayAnalyticsTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.SdReplayTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.PlayerTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.TeamPreviewTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.TeamPreviewPokemonTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.OpenTeamSheetTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.OtsPokemonTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.TerastallizationTransformer
import com.tambapps.pokemon.alakastats.domain.usecase.CreateTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ListTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.infrastructure.repository.KStoreTeamlyticsRepository
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.KStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.createTeamlyticsKStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid
import org.koin.dsl.module

private val appModule = module {
    single { Json { ignoreUnknownKeys = true } }
    single<PokemonImageService> { PokemonImageService(get()) }
    single<PokepasteParser> { PokepasteParser() }
    single<KStorage<Uuid, TeamlyticsEntity>> { createTeamlyticsKStorage(get()) }
    single<TeamlyticsRepository> { KStoreTeamlyticsRepository(get(), get()) }
    single<CreateTeamlyticsUseCase> { CreateTeamlyticsUseCase(get()) }
    single<ListTeamlyticsUseCase> { ListTeamlyticsUseCase(get()) }
    factory { HomeViewModel(get(), get()) }
    factory { CreateTeamViewModel(get(), get()) }
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
}

val appModules = listOf(appModule, transformerModule)