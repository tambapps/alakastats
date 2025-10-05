package com.tambapps.pokemon.alakastats.di

import androidx.compose.runtime.MutableState
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
import com.tambapps.pokemon.alakastats.domain.transformer.TeamlyticsPreviewTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.TerastallizationTransformer
import com.tambapps.pokemon.alakastats.domain.usecase.TeamlyticsUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.EditTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.HandleTeamReplaysUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamlyticsListUseCase
import com.tambapps.pokemon.alakastats.infrastructure.usecase.TeamlyticsUseCaseImpl
import com.tambapps.pokemon.alakastats.infrastructure.usecase.EditTeamlyticsUseCaseImpl
import com.tambapps.pokemon.alakastats.infrastructure.usecase.ManageTeamlyticsListUseCaseImpl
import com.tambapps.pokemon.alakastats.infrastructure.repository.KStoreTeamlyticsRepository
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.KStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.createTeamlyticsKStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.createTeamlyticsPreviewKStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsPreviewEntity
import com.tambapps.pokemon.alakastats.infrastructure.service.ReplayAnalyticsService
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.lead.LeadStatsViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.move.MoveUsageViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.notes.TeamNotesViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.overview.OverviewViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.replay.TeamReplayViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.usage.UsageStatsViewModel
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
            previewTransformer = get()
        ) 
    }

    single<EditTeamlyticsUseCase> { EditTeamlyticsUseCaseImpl(get()) }
    single<ManageTeamlyticsListUseCase> { ManageTeamlyticsListUseCaseImpl(get()) }
    single<TeamlyticsUseCase> { TeamlyticsUseCaseImpl(get()) }

    factory { HomeViewModel(get(), get()) }
    factory { EditTeamViewModel(get(), get(), get()) }
    factory { TeamlyticsViewModel(get(), get()) }
    factory { (teamState: MutableState<Teamlytics?>, team: Teamlytics) ->
        OverviewViewModel(get(), teamState, team)
    }
    factory { (teamState: MutableState<Teamlytics?>, team: Teamlytics) ->
        TeamNotesViewModel(get(), teamState, team)
    }
    factory { (useCase: HandleTeamReplaysUseCase, team: Teamlytics) ->
        TeamReplayViewModel(get(), useCase, team)
    }
    factory { (team: Teamlytics) ->
        LeadStatsViewModel(team, get())
    }
    factory { (team: Teamlytics) ->
        UsageStatsViewModel(team, get())
    }
    factory { (team: Teamlytics) ->
        MoveUsageViewModel(team, get())
    }
}

private val transformerModule = module {
    single { TerastallizationTransformer() }
    single { TeamPreviewPokemonTransformer() }
    single { OtsPokemonTransformer() }
    single { TeamPreviewTransformer(get()) }
    single { OpenTeamSheetTransformer(get()) }
    single { PlayerTransformer(get(), get(), get()) }
    single { ReplayAnalyticsTransformer(get()) }
    single { TeamlyticsTransformer(get(), get()) }
    single { TeamlyticsPreviewTransformer() }
}

val appModules = listOf(appModule, transformerModule)