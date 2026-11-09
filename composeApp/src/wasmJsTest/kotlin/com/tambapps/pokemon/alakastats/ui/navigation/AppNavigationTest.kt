package com.tambapps.pokemon.alakastats.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.runSkikoComposeUiTest
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
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tambapps.pokemon.alakastats.domain.error.GetTeamlyticsError
import com.tambapps.pokemon.alakastats.domain.error.TeamlyticsNotFound
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.domain.model.Player
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.TeamPreview
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsData
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.domain.model.UserName
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.QuizDirection
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.SpeedInteractionsMode
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.TeamlyticsTab
import com.tambapps.pokemon.pokepaste.parser.PokePaste
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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
import kotlin.time.Instant
import kotlin.time.TimeSource
import kotlin.uuid.Uuid

/**
 * Waits for what the app does in the background. waitUntil can't: it blocks the browser's only thread, whose event loop
 * runs the coroutines of the view models
 */
@OptIn(ExperimentalTestApi::class)
private suspend fun ComposeUiTest.awaitUntil(description: String, timeoutMillis: Long = 20_000, condition: () -> Boolean) {
    val start = TimeSource.Monotonic.markNow()
    while (!condition()) {
        if (start.elapsedNow().inWholeMilliseconds > timeoutMillis) throw AssertionError("Timeout: $description")
        withContext(Dispatchers.Default) { delay(20) }
        mainClock.advanceTimeByFrame()
    }
}

// no need to write teams in the browser's storage
private class InMemoryTeamlyticsRepository(teams: List<Teamlytics>) : TeamlyticsRepository {
    private val teamsById = teams.associateBy { it.id }

    override suspend fun get(id: Uuid): Either<GetTeamlyticsError, Teamlytics> =
        teamsById[id]?.right() ?: TeamlyticsNotFound(id).left()

    override suspend fun list() = teamsById.values.toList()
    override suspend fun listPreviews() = emptyList<TeamlyticsPreview>()
    override suspend fun save(teamlytics: Teamlytics, base: Teamlytics?): Either<GetTeamlyticsError, Teamlytics> = teamlytics.right()
    override suspend fun delete(teamlytics: Teamlytics) = Unit
    override suspend fun delete(id: Uuid) = Unit
}

private fun appModulesWithTeams(vararg teams: Teamlytics) = appModules + wasmModule + module {
    single<TeamlyticsRepository> { InMemoryTeamlyticsRepository(teams.toList()) }
}

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
            AppRoute.ShowdownReplays,
            AppRoute.Pokedex(),
            AppRoute.Pokedex("metagross-mega"),
            AppRoute.CloudSync,
            AppRoute.About,
            AppRoute.DamageCalc,
            AppRoute.StandaloneDamageCalc,
            AppRoute.Teamlytics(teamId),
            AppRoute.Teamlytics(Uuid.random(), TeamlyticsTab.GAME_PLANS),
            AppRoute.Teamlytics(Uuid.random(), TeamlyticsTab.REPLAYS, "gen9vgc2026-123"),
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

    private fun ComposeUiTest.goToPart(label: String) {
        onNode(hasContentDescription("Menu")).performClick()
        waitForIdle()
        onNodeWithText(label).performClick()
    }

    @Test
    fun theRealAppGoesFromAPartToAnotherWithTheNavigation() = runComposeUiTest {
        var backStack: List<NavKey> = emptyList()
        setContent {
            KoinApplication(
                configuration = koinConfiguration { modules(appModules + wasmModule) },
                logLevel = Level.NONE,
            ) {
                App(backStackEffect = { backStack = it })
            }
        }
        waitUntil(timeoutMillis = 20_000) { onAllNodes(hasText("Pokédex")).fetchSemanticsNodes().isNotEmpty() }
        goToPart("Pokédex")
        waitUntil(timeoutMillis = 20_000) { onAllNodes(hasText("Search a Pokémon")).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(listOf<NavKey>(AppRoute.Home, AppRoute.Pokedex()), backStack.toList())
        // a part replaces the other one: a back from it goes to the teams
        goToPart("Quizzes")
        waitUntil(timeoutMillis = 20_000) { onAllNodes(hasText("Search a Pokémon")).fetchSemanticsNodes().isEmpty() }
        assertEquals(listOf<NavKey>(AppRoute.Home, AppRoute.Quizzes), backStack.toList())
        goToPart("Teams")
        waitUntil(timeoutMillis = 20_000) { backStack.toList() == listOf<NavKey>(AppRoute.Home) }
    }

    private val partsOfTheApp = listOf(AppRoute.Home, AppRoute.ShowdownReplays, AppRoute.DamageCalc, AppRoute.Quizzes, AppRoute.Pokedex("metagross"))
    private val screensBelowTheParts = listOf(AppRoute.StandaloneDamageCalc, AppRoute.NatureQuizSetup, AppRoute.Teamlytics(Uuid.random()), AppRoute.About)

    // the drawer's content is there even when it's closed: its menu button is what tells
    private fun menuButtonIsOnlyOnTheParts(isCompact: Boolean) = runComposeUiTest {
        val backStack = NavBackStack<NavKey>(AppRoute.Home)
        setContent {
            CompositionLocalProvider(LocalIsCompact provides isCompact) {
                WithAppNavigationDrawer(backStack, AppNavigator(backStack)) {}
            }
        }
        for (route in partsOfTheApp) {
            backStack[backStack.lastIndex] = route
            waitForIdle()
            onNode(hasContentDescription("Menu")).assertExists()
        }
        for (route in screensBelowTheParts) {
            backStack[backStack.lastIndex] = route
            waitForIdle()
            onNode(hasContentDescription("Menu")).assertDoesNotExist()
        }
    }

    @Test
    fun onMobileTheMenuButtonIsOnlyOnThePartsOfTheApp() = menuButtonIsOnlyOnTheParts(isCompact = true)

    @Test
    fun onDesktopTheMenuButtonIsOnlyOnThePartsOfTheApp() = menuButtonIsOnlyOnTheParts(isCompact = false)

    private fun drawerIsClosedUntilTheMenuButtonOpensIt(isCompact: Boolean) = runComposeUiTest {
        val backStack = NavBackStack<NavKey>(AppRoute.Home)
        setContent {
            CompositionLocalProvider(LocalIsCompact provides isCompact) {
                WithAppNavigationDrawer(backStack, AppNavigator(backStack)) {}
            }
        }
        onNodeWithText("Damage Calc").assertIsNotDisplayed()
        onNode(hasContentDescription("Menu")).performClick()
        waitForIdle()
        onNodeWithText("Damage Calc").assertIsDisplayed().performClick()
        waitForIdle()
        assertEquals(listOf<NavKey>(AppRoute.Home, AppRoute.DamageCalc), backStack.toList())
        onNodeWithText("Damage Calc").assertIsNotDisplayed()
    }

    @Test
    fun onMobileTheDrawerIsClosedUntilTheMenuButtonOpensIt() = drawerIsClosedUntilTheMenuButtonOpensIt(isCompact = true)

    @Test
    fun onDesktopTheDrawerIsClosedUntilTheMenuButtonOpensIt() = drawerIsClosedUntilTheMenuButtonOpensIt(isCompact = false)

    @Test
    fun aPartOfTheAppIsRightAboveTheRoot() {
        val backStack = mutableListOf<NavKey>(AppRoute.Home, AppRoute.Quizzes, AppRoute.NatureQuizSetup)
        val navigator = AppNavigator(backStack)
        navigator.navigateToTopLevel(AppRoute.Pokedex())
        assertEquals(listOf<NavKey>(AppRoute.Home, AppRoute.Pokedex()), backStack)
        navigator.navigateToTopLevel(AppRoute.ShowdownReplays)
        assertEquals(listOf<NavKey>(AppRoute.Home, AppRoute.ShowdownReplays), backStack)
        navigator.navigateToTopLevel(AppRoute.Home)
        assertEquals(listOf<NavKey>(AppRoute.Home), backStack)
    }

    @Test
    fun anUnknownTeamGoesToTheHomeScreen() = runComposeUiTest {
        var backStack: List<NavKey> = emptyList()
        setContent {
            KoinApplication(
                configuration = koinConfiguration { modules(appModulesWithTeams()) },
                logLevel = Level.NONE,
            ) {
                App(
                    startRoutes = listOf(AppRoute.Home, AppRoute.Teamlytics(Uuid.random())),
                    backStackEffect = { backStack = it },
                )
            }
        }
        awaitUntil("the back stack is the home screen") { backStack.toList() == listOf<NavKey>(AppRoute.Home) }
        awaitUntil("the home screen is shown") { onAllNodes(hasText("Pokédex")).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun emptyTeam() = Teamlytics(
        id = Uuid.random(),
        name = "Team",
        pokePaste = PokePaste(emptyList()),
        replays = emptyList(),
        sdNames = listOf(UserName("me")),
        lastUpdatedAt = Instant.fromEpochMilliseconds(0),
        data = TeamlyticsData(emptyMap()),
        notes = null,
        matchupPlans = emptyList(),
        format = Format.NONE,
    )

    private val aTeam = emptyTeam()
    private val unknownTeamId = Uuid.random()

    private fun goesBackTo(expected: List<AppRoute>, teams: List<Teamlytics>, startRoutes: List<AppRoute>) = runComposeUiTest {
        var backStack: List<NavKey> = emptyList()
        setContent {
            KoinApplication(
                configuration = koinConfiguration { modules(appModulesWithTeams(*teams.toTypedArray())) },
                logLevel = Level.NONE,
            ) {
                App(startRoutes = startRoutes, backStackEffect = { backStack = it })
            }
        }
        awaitUntil("the back stack is $expected") { backStack.toList() == expected }
    }

    // these tests must return goesBackTo's promise: without it, they'd end at once and go on during the next test
    @Test
    fun aPokemonOfAnUnknownTeamGoesToTheHomeScreen() = goesBackTo(
        expected = listOf(AppRoute.Home),
        teams = emptyList(),
        startRoutes = listOf(AppRoute.Home, AppRoute.Teamlytics(unknownTeamId), AppRoute.PokemonDetails(unknownTeamId, "metagross")),
    )

    @Test
    fun aPokemonThatIsntOnTheTeamGoesToTheTeam() = goesBackTo(
        expected = listOf(AppRoute.Home, AppRoute.Teamlytics(aTeam.id)),
        teams = listOf(aTeam),
        startRoutes = listOf(AppRoute.Home, AppRoute.Teamlytics(aTeam.id), AppRoute.PokemonDetails(aTeam.id, "metagross")),
    )

    @Test
    fun theQuizOfAnUnknownTeamGoesToTheHomeScreen() = goesBackTo(
        expected = listOf(AppRoute.Home),
        teams = emptyList(),
        startRoutes = listOf(AppRoute.Home, AppRoute.Teamlytics(unknownTeamId, TeamlyticsTab.QUIZZES), AppRoute.SpeedInteractionsTeamSetup(unknownTeamId)),
    )

    @Test
    fun theTabsOfATeamFollowTheRouteAndTheRouteTheTabs() = runComposeUiTest {
        val team = emptyTeam()
        var backStack: MutableList<NavKey>? = null
        setContent {
            KoinApplication(
                configuration = koinConfiguration { modules(appModulesWithTeams(team)) },
                logLevel = Level.NONE,
            ) {
                App(
                    // opened on a tab
                    startRoutes = listOf(AppRoute.Home, AppRoute.Teamlytics(team.id, TeamlyticsTab.LEAD_STATS)),
                    backStackEffect = { backStack = it },
                )
            }
        }
        val tab = { title: String -> hasText(title) and hasClickAction() }
        awaitUntil("the team is loaded") { onAllNodes(tab("Lead Stats")).fetchSemanticsNodes().isNotEmpty() }
        onNode(tab("Lead Stats")).assertIsSelected()

        // the tabs change the route
        onNode(tab("Usages")).performClick()
        awaitUntil("the route follows the tab") { backStack?.lastOrNull() == AppRoute.Teamlytics(team.id, TeamlyticsTab.USAGES) }
        onNode(tab("Usages")).assertIsSelected()

        // the route changes the tab, as the browser's buttons do
        backStack!![backStack!!.lastIndex] = AppRoute.Teamlytics(team.id, TeamlyticsTab.REPLAYS)
        awaitUntil("the tab follows the route") {
            onAllNodes(tab("Replays") and isSelected()).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals<NavKey?>(AppRoute.Teamlytics(team.id, TeamlyticsTab.REPLAYS), backStack?.lastOrNull())
    }

    @Test
    fun onDesktopTheReplaysTabScrollsToTheAskedReplay() = theReplaysTabScrollsToTheAskedReplay(Size(1920f, 1080f))

    @Test
    fun onMobileTheReplaysTabScrollsToTheAskedReplay() = theReplaysTabScrollsToTheAskedReplay(Size(800f, 1200f))

    private fun theReplaysTabScrollsToTheAskedReplay(windowSize: Size) = runSkikoComposeUiTest(windowSize) {
        fun player(name: String) = Player(
            name = UserName(name),
            teamPreview = TeamPreview(emptyList()),
            selection = emptyList(),
            beforeElo = null,
            afterElo = null,
            terastallization = null,
            megaEvolution = null,
            ots = null,
            movesUsage = emptyMap(),
        )
        val replays = (0 until 60).map { index ->
            ReplayAnalytics(
                players = listOf(player("me"), player("opponent-$index")),
                uploadTime = index.toLong(),
                format = "",
                rating = null,
                version = "1",
                winner = UserName("me"),
                url = null,
                reference = "ref-$index",
                nextBattleRef = null,
                notes = null,
            )
        }
        val team = Teamlytics(
            id = Uuid.random(),
            name = "Team",
            pokePaste = PokePaste(emptyList()),
            replays = replays,
            sdNames = listOf(UserName("me")),
            lastUpdatedAt = Instant.fromEpochMilliseconds(0),
            data = TeamlyticsData(emptyMap()),
            notes = null,
            matchupPlans = emptyList(),
            format = Format.NONE,
        )
        var backStack: MutableList<NavKey>? = null
        setContent {
            KoinApplication(
                configuration = koinConfiguration { modules(appModulesWithTeams(team)) },
                logLevel = Level.NONE,
            ) {
                App(
                    startRoutes = listOf(AppRoute.Home, AppRoute.Teamlytics(team.id, TeamlyticsTab.REPLAYS, "ref-30")),
                    backStackEffect = { backStack = it },
                )
            }
        }
        awaitUntil("the replay is shown") {
            onAllNodes(hasText("opponent-30", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        // the list did move: the replays at its ends aren't laid out
        assertEquals(0, onAllNodes(hasText("opponent-59", substring = true)).fetchSemanticsNodes().size)
        assertEquals(0, onAllNodes(hasText("opponent-0", substring = true)).fetchSemanticsNodes().size)

        onNode(hasText("Usages") and hasClickAction()).performClick()
        awaitUntil("the replay isn't in the route anymore") {
            backStack?.lastOrNull() == AppRoute.Teamlytics(team.id, TeamlyticsTab.USAGES)
        }
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

    @Test fun homeOpens() = screenOpens(AppRoute.Home)
    @Test fun showdownReplaysOpens() = screenOpens(AppRoute.ShowdownReplays)
    @Test fun pokedexOpens() = screenOpens(AppRoute.Pokedex())
    @Test fun pokedexOpensOnAPokemon() = screenOpens(AppRoute.Pokedex("metagross-mega"))
    @Test fun aboutOpens() = screenOpens(AppRoute.About)
    @Test fun cloudSyncOpens() = screenOpens(AppRoute.CloudSync)
    @Test fun damageCalcOpens() = screenOpens(AppRoute.DamageCalc)
    @Test fun standaloneDamageCalcOpens() = screenOpens(AppRoute.StandaloneDamageCalc)
    @Test fun teamlyticsOpens() = screenOpens(AppRoute.Teamlytics(Uuid.random()))
    @Test fun teamlyticsOpensOnATab() = screenOpens(AppRoute.Teamlytics(Uuid.random(), TeamlyticsTab.REPLAYS))
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
