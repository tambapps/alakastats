package com.tambapps.pokemon.alakastats.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEffect
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.alakastats.App
import com.tambapps.pokemon.alakastats.di.appModules
import com.tambapps.pokemon.alakastats.di.wasmModule
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.QuizDirection
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.SpeedInteractionsMode
import kotlinx.serialization.PolymorphicSerializer
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.stopKoin
import org.koin.core.logger.Level
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

private class RecordingViewModel(val id: Uuid) : ViewModel() {
    var clicks by mutableStateOf(0)

    override fun onCleared() {
        cleared += id
    }

    companion object {
        val cleared = mutableListOf<Uuid>()
    }
}

@OptIn(ExperimentalTestApi::class)
class AppNavigationTest {

    // Koin's context is global: the next test would reuse this one's modules
    @AfterTest
    fun stopKoinContext() = stopKoin()

    @Test
    fun theBackStackIsSavedAndRestored() {
        val serializer = NavBackStackSerializer(PolymorphicSerializer(NavKey::class))
        val teamId = Uuid.random()
        val routes = listOf(
            AppRoute.Home,
            AppRoute.Pokedex(),
            AppRoute.Pokedex("metagross-mega"),
            AppRoute.CloudSync,
            AppRoute.About,
            AppRoute.DamageCalc,
            AppRoute.StandaloneDamageCalc,
            AppRoute.Teamlytics(teamId),
            AppRoute.Teamlytics(Uuid.random()),
            AppRoute.EditTeam(),
            AppRoute.EditTeam(teamId, redirectToTeamlytics = true),
            AppRoute.PokemonDetails(teamId, "metagross-mega"),
            AppRoute.ManualReplay(teamId),
            AppRoute.ManualReplay(teamId, "manual-abc"),
            AppRoute.Quizzes,
            AppRoute.NatureQuizSetup,
            AppRoute.NatureQuiz(listOf(Nature.ADAMANT, Nature.TIMID), QuizDirection.STATS_TO_NAME),
            AppRoute.SpeedStatQuizSetup,
            AppRoute.SpeedStatQuiz(Format.REGULATION_MC, true, false, true),
            AppRoute.SpeedInteractionsHomeSetup,
            AppRoute.SpeedInteractionsTeamSetup(teamId),
            AppRoute.SpeedInteractionsQuiz(SpeedInteractionsMode.Home(Format.REGULATION_I)),
            AppRoute.SpeedInteractionsQuiz(SpeedInteractionsMode.Team(teamId, allowTailwind = true)),
        )
        val saved = encodeToSavedState(serializer, NavBackStack<NavKey>(*routes.toTypedArray()), appRouteSavedStateConfiguration)
        val restored = decodeFromSavedState(serializer, saved, appRouteSavedStateConfiguration)
        assertEquals<List<NavKey>>(routes, restored.toList())
    }

    @Test
    fun eachEntryHasItsOwnViewModelAndStateClearedWhenPopped() = runComposeUiTest {
        RecordingViewModel.cleared.clear()
        val first = Uuid.random()
        val second = Uuid.random()
        val backStack = NavBackStack<NavKey>(AppRoute.Teamlytics(first))
        setContent {
            KoinApplication(
                configuration = koinConfiguration {
                    modules(module { factory { (id: Uuid) -> RecordingViewModel(id) } })
                },
                logLevel = Level.NONE,
            ) {
                NavDisplay(
                    backStack = backStack,
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    entryProvider = entryProvider {
                        entry<AppRoute.Teamlytics> { route ->
                            val viewModel = koinViewModel<RecordingViewModel> { parametersOf(route.teamId) }
                            var saved by rememberSaveable { mutableIntStateOf(0) }
                            Column {
                                Text("vm=${viewModel.id} vmClicks=${viewModel.clicks} saved=$saved")
                                Text("click", Modifier.clickable {
                                    viewModel.clicks++
                                    saved++
                                })
                            }
                        }
                    },
                )
            }
        }
        onNodeWithText("vm=$first vmClicks=0 saved=0").assertExists()
        onNodeWithText("click").performClick()
        onNodeWithText("vm=$first vmClicks=1 saved=1").assertExists()

        // the same screen twice in the stack, with different teams
        backStack.add(AppRoute.Teamlytics(second))
        waitForIdle()
        onNodeWithText("vm=$second vmClicks=0 saved=0").assertExists()
        assertEquals(emptyList(), RecordingViewModel.cleared)

        backStack.removeLastOrNull()
        waitForIdle()
        onNodeWithText("vm=$first vmClicks=1 saved=1").assertExists()
        assertEquals(listOf(second), RecordingViewModel.cleared)
    }

    @Test
    fun replacingARouteOfTheSameContentKeepsItsViewModelAndUpdatesTheScreen() = runComposeUiTest {
        RecordingViewModel.cleared.clear()
        val id = Uuid.random()
        val backStack = NavBackStack<NavKey>(AppRoute.Pokedex("first"))
        setContent {
            KoinApplication(
                configuration = koinConfiguration {
                    modules(module { factory { (id: Uuid) -> RecordingViewModel(id) } })
                },
                logLevel = Level.NONE,
            ) {
                NavDisplay(
                    backStack = backStack,
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    entryProvider = entryProvider {
                        entry<AppRoute.Pokedex>(clazzContentKey = { "pokedex" }) { route ->
                            val viewModel = koinViewModel<RecordingViewModel> { parametersOf(id) }
                            Column {
                                Text("form=${route.form} clicks=${viewModel.clicks}")
                                Text("click", Modifier.clickable { viewModel.clicks++ })
                            }
                        }
                    },
                )
            }
        }
        onNodeWithText("click").performClick()
        onNodeWithText("form=first clicks=1").assertExists()

        backStack[0] = AppRoute.Pokedex("second")
        waitForIdle()
        onNodeWithText("form=second clicks=1").assertExists()
        assertEquals(emptyList(), RecordingViewModel.cleared)
    }

    @Test
    fun aResultReachesThePreviousScreenOnceBackToIt() = runComposeUiTest {
        val backStack = NavBackStack<NavKey>(AppRoute.Home)
        val received = mutableListOf<String>()
        setContent {
            NavDisplay(
                backStack = backStack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberResultEventBusNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<AppRoute.Home> {
                        ResultEffect<String> { received += it }
                        Text("previous")
                    }
                    entry<AppRoute.About> {
                        val resultEventBus = LocalResultEventBus.current
                        Text("send", Modifier.clickable {
                            resultEventBus.sendResult("the result")
                            backStack.removeLastOrNull()
                        })
                    }
                },
            )
        }
        backStack.add(AppRoute.About)
        waitForIdle()
        assertEquals(emptyList(), received)

        onNodeWithText("send").performClick()
        waitForIdle()
        onNodeWithText("previous").assertExists()
        assertEquals(listOf("the result"), received)
    }

    @Test
    fun theRealAppNavigatesToThePokedexAndBack() = runComposeUiTest {
        setContent {
            KoinApplication(
                configuration = koinConfiguration { modules(appModules + wasmModule) },
                logLevel = Level.NONE,
            ) {
                App()
            }
        }
        waitUntil(timeoutMillis = 20_000) { onAllNodes(hasText("Pokédex")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Pokédex").performClick()
        waitUntil(timeoutMillis = 20_000) { onAllNodes(hasText("Search a Pokémon")).fetchSemanticsNodes().isNotEmpty() }
        onNode(hasContentDescription("Back")).performClick()
        waitUntil(timeoutMillis = 20_000) { onAllNodes(hasText("Search a Pokémon")).fetchSemanticsNodes().isEmpty() }
        waitUntil(timeoutMillis = 20_000) { onAllNodes(hasText("Pokédex")).fetchSemanticsNodes().isNotEmpty() }
    }

    // each screen in the real app: its view model must be resolved by Koin, with its parameters
    private fun screenOpens(route: AppRoute) = runComposeUiTest {
        // the loading indicators animate forever: it would never be idle
        mainClock.autoAdvance = false
        setContent {
            KoinApplication(
                configuration = koinConfiguration { modules(appModules + wasmModule) },
                logLevel = Level.NONE,
            ) {
                App(startRoutes = listOf(route))
            }
        }
        mainClock.advanceTimeBy(1_000)
    }

    @Test fun pokedexOpens() = screenOpens(AppRoute.Pokedex())
    @Test fun pokedexOpensOnAPokemon() = screenOpens(AppRoute.Pokedex("metagross-mega"))
    @Test fun aboutOpens() = screenOpens(AppRoute.About)
    @Test fun cloudSyncOpens() = screenOpens(AppRoute.CloudSync)
    @Test fun damageCalcOpens() = screenOpens(AppRoute.DamageCalc)
    @Test fun standaloneDamageCalcOpens() = screenOpens(AppRoute.StandaloneDamageCalc)
    @Test fun teamlyticsOpens() = screenOpens(AppRoute.Teamlytics(Uuid.random()))
    @Test fun createTeamOpens() = screenOpens(AppRoute.EditTeam())
    @Test fun editTeamOpens() = screenOpens(AppRoute.EditTeam(Uuid.random(), redirectToTeamlytics = true))
    @Test fun pokemonDetailsOpens() = screenOpens(AppRoute.PokemonDetails(Uuid.random(), "metagross"))
    @Test fun manualReplayOpens() = screenOpens(AppRoute.ManualReplay(Uuid.random()))
    @Test fun quizzesOpens() = screenOpens(AppRoute.Quizzes)
    @Test fun natureQuizSetupOpens() = screenOpens(AppRoute.NatureQuizSetup)
    @Test fun natureQuizOpens() = screenOpens(AppRoute.NatureQuiz(listOf(Nature.ADAMANT), QuizDirection.NAME_TO_STATS))
    @Test fun speedStatQuizSetupOpens() = screenOpens(AppRoute.SpeedStatQuizSetup)
    @Test fun speedStatQuizOpens() = screenOpens(AppRoute.SpeedStatQuiz(Format.REGULATION_MC, false, false, false))
    @Test fun speedInteractionsHomeSetupOpens() = screenOpens(AppRoute.SpeedInteractionsHomeSetup)
    @Test fun speedInteractionsTeamSetupOpens() = screenOpens(AppRoute.SpeedInteractionsTeamSetup(Uuid.random()))
    @Test fun speedInteractionsQuizOpens() = screenOpens(AppRoute.SpeedInteractionsQuiz(SpeedInteractionsMode.Home(Format.REGULATION_MC)))
}
