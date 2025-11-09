package com.tambapps.pokemon.alakastats.di

import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import com.tambapps.pokemon.alakastats.ui.screen.home.HomeViewModel
import com.tambapps.pokemon.alakastats.ui.screen.editteam.EditTeamViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.TeamlyticsViewModel
import com.tambapps.pokemon.alakastats.domain.transformer.TeamlyticsTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.ReplayAnalyticsTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.PlayerTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.TeamPreviewTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.TeamPreviewPokemonTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.OpenTeamSheetTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.OtsPokemonTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.TeamlyticsNotesTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.TeamlyticsPreviewTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.TerastallizationTransformer
import com.tambapps.pokemon.alakastats.domain.usecase.HandleTeamOverviewUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.HandleTeamReplaysUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.TeamlyticsUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.EditTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamlyticsListUseCase
import com.tambapps.pokemon.alakastats.infrastructure.repository.KStoreTeamlyticsRepository
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.KStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.createTeamlyticsKStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.createTeamlyticsPreviewKStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsPreviewEntity
import com.tambapps.pokemon.alakastats.infrastructure.service.ReplayAnalyticsService
import com.tambapps.pokemon.alakastats.infrastructure.service.TeamlyticsSerializer
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.lead.LeadStatsViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.move.UsagesViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.overview.OverviewViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.replay.TeamReplayViewModel
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid
import org.koin.core.qualifier.named
import org.koin.dsl.module
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.core.module.dsl.singleOf

private val appModule = module {
    single { Json { ignoreUnknownKeys = true } }
    single<HttpClient> {
        HttpClient {
            install(ContentNegotiation) {
                json(get<Json>())
            }
        }
    }
    single<PokepasteParser> { PokepasteParser() }
    single<ReplayAnalyticsService> { ReplayAnalyticsService(get(), get(), get()) }

    // need to name them as they have both the same signature after generic type erasure
    single<KStorage<Uuid, TeamlyticsEntity>>(named("teamsStorage")) { createTeamlyticsKStorage() }
    single<KStorage<Uuid, TeamlyticsPreviewEntity>>(named("previewsStorage")) { createTeamlyticsPreviewKStorage() }
    single<TeamlyticsRepository> { 
        KStoreTeamlyticsRepository(
            teamsStorage = get(named("teamsStorage")), 
            previewsStorage = get(named("previewsStorage")), 
            teamlyticsTransformer = get(), 
            previewTransformer = get(),
        )
    }

    singleOf(::EditTeamlyticsUseCase)
    singleOf(::ManageTeamlyticsListUseCase)
    singleOf(::TeamlyticsUseCase)
    singleOf(::TeamlyticsSerializer)

    factory { HomeViewModel(get(), get()) }
    factory { EditTeamViewModel(get(), get(), get()) }
    factory { TeamlyticsViewModel(get(), get(), get()) }
    factory { (useCase: HandleTeamOverviewUseCase, team: Teamlytics) ->
        OverviewViewModel(useCase, get(), team)
    }
    factory { (useCase: HandleTeamReplaysUseCase, team: Teamlytics) ->
        TeamReplayViewModel(get(), useCase, team)
    }
    factory { (team: Teamlytics) ->
        LeadStatsViewModel(team, get())
    }
    factory { (team: Teamlytics) ->
        UsagesViewModel(team, get())
    }
}

private val transformerModule = module {
    singleOf(::TerastallizationTransformer)
    singleOf(::TeamPreviewPokemonTransformer)
    singleOf(::OtsPokemonTransformer)
    singleOf(::TeamPreviewTransformer)
    singleOf(::OpenTeamSheetTransformer)
    singleOf(::PlayerTransformer)
    singleOf(::ReplayAnalyticsTransformer)
    singleOf(::TeamlyticsTransformer)
    singleOf(::TeamlyticsNotesTransformer)
    singleOf(::TeamlyticsPreviewTransformer)
}

val appModules = listOf(appModule, transformerModule)