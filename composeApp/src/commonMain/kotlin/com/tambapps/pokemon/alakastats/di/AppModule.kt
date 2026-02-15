package com.tambapps.pokemon.alakastats.di

import com.tambapps.pokemon.alakastats.domain.repository.PokemonDataRepository
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.alakastats.domain.transformer.GamePlanTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.MatchupNotesTransformer
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
import com.tambapps.pokemon.alakastats.domain.usecase.ConsultTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamOverviewUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamReplaysUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.EditTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageMatchupNotesUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamlyticsListUseCase
import com.tambapps.pokemon.alakastats.infrastructure.repository.KStoreTeamlyticsRepository
import com.tambapps.pokemon.alakastats.infrastructure.repository.PokeApiPokemonDataRepository
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.KStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.createTeamlyticsKStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.createTeamlyticsPreviewKStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsPreviewEntity
import com.tambapps.pokemon.alakastats.infrastructure.service.ReplayAnalyticsService
import com.tambapps.pokemon.alakastats.infrastructure.service.TeamlyticsSerializer
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.lead.LeadStatsViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.matchup.MatchupNotesViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.matchup.edit.MatchupNotesEditViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.usage.UsagesViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.overview.OverviewViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.replay.TeamReplayViewModel
import com.tambapps.pokemon.pokeapi.client.PokeApiGqlClient
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid
import org.koin.core.qualifier.named
import org.koin.dsl.module
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind

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
    single { PokeApiGqlClient(get()) }
    singleOf(::PokeApiPokemonDataRepository).bind(PokemonDataRepository::class)

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
    singleOf(::ManageTeamlyticsUseCase)
    singleOf(::TeamlyticsSerializer)

    factoryOf(::HomeViewModel)
    factoryOf(::EditTeamViewModel)
    factoryOf(::MatchupNotesEditViewModel)

    factory { (teamId: Uuid) ->
        TeamlyticsViewModel(teamId, get(), get())
    }
    factory { (useCase: ManageTeamOverviewUseCase) ->
        OverviewViewModel(useCase, get(), get())
    }
    factory { (useCase: ManageTeamReplaysUseCase) ->
        TeamReplayViewModel(useCase, get(), get())
    }
    factory { (useCase: ManageTeamReplaysUseCase) ->
        LeadStatsViewModel(useCase, get())
    }
    factory { (useCase: ManageTeamReplaysUseCase) ->
        UsagesViewModel(useCase, get())
    }
    factory { (useCase: ManageMatchupNotesUseCase) ->
        MatchupNotesViewModel(useCase, get())
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
    singleOf(::MatchupNotesTransformer)
    singleOf(::GamePlanTransformer)
}

val appModules = listOf(appModule, transformerModule)