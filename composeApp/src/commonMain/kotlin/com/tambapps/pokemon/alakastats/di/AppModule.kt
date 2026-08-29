package com.tambapps.pokemon.alakastats.di

import androidx.compose.runtime.MutableState
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.FormatData
import com.tambapps.pokemon.alakastats.domain.repository.FormatDataRepository
import com.tambapps.pokemon.alakastats.domain.repository.PokemonBaseStatsRepository
import com.tambapps.pokemon.alakastats.domain.repository.PokemonMovesRepository
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.alakastats.domain.usecase.ConsultPokemonDetailUseCase
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import com.tambapps.pokemon.alakastats.ui.screen.home.HomeViewModel
import com.tambapps.pokemon.alakastats.ui.screen.editteam.EditTeamViewModel
import com.tambapps.pokemon.alakastats.ui.screen.manualreplay.ManualReplayViewModel
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.QuizzesViewModel
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.NatureQuizSetupViewModel
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.NatureQuizViewModel
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.QuizDirection
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.SpeedStatQuizSetupViewModel
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.SpeedStatQuizViewModel
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.SpeedInteractionsHomeSetupViewModel
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.SpeedInteractionsMode
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.SpeedInteractionsTeamSetupViewModel
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.SpeedInteractionsViewModel
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.usecase.ConsultTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.quizzes.QuizzesTabViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.TeamlyticsViewModel
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamOverviewUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamReplaysUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.EditTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageMatchupPlansUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageReplayFiltersUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamlyticsListUseCase
import com.tambapps.pokemon.alakastats.infrastructure.repository.KStoreTeamlyticsRepository
import com.tambapps.pokemon.alakastats.infrastructure.repository.LocalFormatDataRepository
import com.tambapps.pokemon.alakastats.infrastructure.repository.LocalPokemonBaseStatsRepository
import com.tambapps.pokemon.alakastats.infrastructure.repository.PokeApiPokemonMovesRepository
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.KStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.createTeamlyticsKStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.createTeamlyticsPreviewKStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsPreviewEntity
import com.tambapps.pokemon.alakastats.infrastructure.service.ReplayAnalyticsService
import com.tambapps.pokemon.alakastats.infrastructure.service.TeamlyticsSerializer
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail.PokemonDetailViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail.TeamPokemonStateState
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail.tabs.overview.PokemonDetailOverviewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail.tabs.speedscale.PokemonSpeedScaleViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.lead.LeadStatsViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.gameplan.MatchupPlansViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.gameplan.edit.MatchupPlanEditViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.opponent.MatchupsViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.usage.UsagesViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.overview.OverviewViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.replay.TeamReplayViewModel
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

val appModules = listOf(module {
    single { Json { ignoreUnknownKeys = true } }
    single<HttpClient> {
        HttpClient {
            install(ContentNegotiation) {
                json(get<Json>())
            }
        }
    }
    single<PokepasteParser> { PokepasteParser() }
    single<ReplayAnalyticsService> { ReplayAnalyticsService(get()) }
    single { PokeApiGqlClient(get()) }
    singleOf(::LocalPokemonBaseStatsRepository).bind(PokemonBaseStatsRepository::class)
    singleOf(::PokeApiPokemonMovesRepository).bind(PokemonMovesRepository::class)
    singleOf(::LocalFormatDataRepository).bind(FormatDataRepository::class)

    // need to name them as they have both the same signature after generic type erasure
    single<KStorage<Uuid, TeamlyticsEntity>>(named("teamsStorage")) { createTeamlyticsKStorage() }
    single<KStorage<Uuid, TeamlyticsPreviewEntity>>(named("previewsStorage")) { createTeamlyticsPreviewKStorage() }
    single<TeamlyticsRepository> {
        KStoreTeamlyticsRepository(
            teamsStorage = get(named("teamsStorage")),
            previewsStorage = get(named("previewsStorage")),
            pokepasteParser = get(),
        )
    }

    singleOf(::EditTeamlyticsUseCase)
    singleOf(::ConsultPokemonDetailUseCase)
    singleOf(::ManageTeamlyticsListUseCase)
    singleOf(::ManageTeamlyticsUseCase)
    singleOf(::TeamlyticsSerializer)

    factoryOf(::HomeViewModel)
    factoryOf(::EditTeamViewModel)
    factoryOf(::MatchupPlanEditViewModel)
    factoryOf(::QuizzesViewModel)
    factoryOf(::NatureQuizSetupViewModel)
    factoryOf(::SpeedStatQuizSetupViewModel)
    factoryOf(::SpeedInteractionsHomeSetupViewModel)

    factory { (natures: List<Nature>, direction: QuizDirection) ->
        NatureQuizViewModel(natures, direction)
    }

    factory { (format: Format, allowChoiceScarf: Boolean, useMaxStatPoints: Boolean, useSpeedBoostingNature: Boolean) ->
        SpeedStatQuizViewModel(format, allowChoiceScarf, useMaxStatPoints, useSpeedBoostingNature, get(), get())
    }

    factory { (mode: SpeedInteractionsMode) ->
        SpeedInteractionsViewModel(mode, get(), get(), get())
    }

    factory { (teamId: Uuid) ->
        SpeedInteractionsTeamSetupViewModel(teamId, get())
    }

    factory { (useCase: ConsultTeamlyticsUseCase) ->
        QuizzesTabViewModel(useCase)
    }

    factory { (teamId: Uuid) ->
        TeamlyticsViewModel(teamId, get(), get())
    }
    factory { (team: Teamlytics) ->
        ManualReplayViewModel(team)
    }
    factory { (teamId: Uuid, pokemonName: PokemonName) ->
        PokemonDetailViewModel(teamId, pokemonName, get())
    }
    factory { (useCase: ManageTeamOverviewUseCase) ->
        OverviewViewModel(useCase, get(), get())
    }
    factory { (state: TeamPokemonStateState.Loaded, megaSelectedState: MutableState<Boolean>) ->
        PokemonDetailOverviewModel(state.team, state.pokemon, state.megaPokemon, state.pokemonData, state.notes, state.usages, megaSelectedState)
    }
    factory { (state: TeamPokemonStateState.Loaded, megaSelectedState: MutableState<Boolean>) ->
        PokemonSpeedScaleViewModel(state.team, state.pokemon, state.megaPokemon, megaSelectedState.value, state.pokemonData, get(), get())
    }
    factory { (useCase: ManageTeamReplaysUseCase, formatData: FormatData?) ->
        TeamReplayViewModel(useCase, get(), formatData)
    }
    factory { (useCase: ManageReplayFiltersUseCase, formatData: FormatData?) ->
        LeadStatsViewModel(useCase, formatData)
    }
    factory { (useCase: ManageTeamReplaysUseCase, formatData: FormatData?) ->
        UsagesViewModel(useCase, formatData)
    }
    factory { (useCase: ManageReplayFiltersUseCase, formatData: FormatData?) ->
        MatchupsViewModel(useCase, formatData)
    }
    factory { (useCase: ManageMatchupPlansUseCase) ->
        MatchupPlansViewModel(useCase)
    }
})
