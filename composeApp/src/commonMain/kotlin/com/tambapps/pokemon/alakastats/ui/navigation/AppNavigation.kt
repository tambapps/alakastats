package com.tambapps.pokemon.alakastats.ui.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.ui.screen.cloudsync.CloudSyncScreen
import com.tambapps.pokemon.alakastats.ui.screen.home.HomeScreen
import com.tambapps.pokemon.alakastats.ui.screen.manualreplay.ManualReplayScreen
import com.tambapps.pokemon.alakastats.ui.screen.pokedex.PokedexScreen
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.QuizDirection
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.SpeedInteractionsMode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclassesOfSealed
import kotlin.uuid.Uuid

@Serializable
sealed interface AppRoute : NavKey {
    @Serializable
    data object Home : AppRoute

    @Serializable
    data object Pokedex : AppRoute

    @Serializable
    data object CloudSync : AppRoute

    @Serializable
    data object About : AppRoute

    @Serializable
    data object DamageCalc : AppRoute

    @Serializable
    data class Teamlytics(val teamId: Uuid) : AppRoute

    /** @param teamId null to create a team */
    @Serializable
    data class EditTeam(val teamId: Uuid? = null, val redirectToTeamlytics: Boolean = false) : AppRoute

    @Serializable
    data class PokemonDetails(val teamId: Uuid, val pokemonName: String) : AppRoute

    /** @param replayReference the manual replay to edit, null to create one */
    @Serializable
    data class ManualReplay(val teamId: Uuid, val replayReference: String? = null) : AppRoute

    @Serializable
    data object Quizzes : AppRoute

    @Serializable
    data object NatureQuizSetup : AppRoute

    @Serializable
    data class NatureQuiz(val natures: List<Nature>, val direction: QuizDirection) : AppRoute

    @Serializable
    data object SpeedStatQuizSetup : AppRoute

    @Serializable
    data class SpeedStatQuiz(
        val format: Format,
        val allowChoiceScarf: Boolean,
        val useMaxStatPoints: Boolean,
        val useSpeedBoostingNature: Boolean,
    ) : AppRoute

    @Serializable
    data object SpeedInteractionsHomeSetup : AppRoute

    @Serializable
    data class SpeedInteractionsTeamSetup(val teamId: Uuid) : AppRoute

    @Serializable
    data class SpeedInteractionsQuiz(val mode: SpeedInteractionsMode) : AppRoute

    // SPIKE: stands for TeamlyticsScreen, to check that two teams in the stack don't share their view model
    @Serializable
    data class Team(val teamId: Uuid) : AppRoute

    // SPIKE: the screens that aren't migrated
    @Serializable
    data class NotMigrated(val name: String) : AppRoute
}

class AppNavigator(private val backStack: MutableList<NavKey>) {
    fun push(route: AppRoute) {
        backStack.add(route)
    }

    /** Never removes the root screen */
    fun pop() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    fun replace(route: AppRoute) {
        backStack[backStack.lastIndex] = route
    }
}

val LocalAppNavigator = staticCompositionLocalOf<AppNavigator> { error("No AppNavigator provided") }

@OptIn(ExperimentalSerializationApi::class)
val appRouteSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            // rather than a list of the routes, which a new one could be missing from: it would crash when restored
            subclassesOfSealed<AppRoute>()
        }
    }
}

@Composable
fun AppNavDisplay() {
    val backStack = rememberNavBackStack(appRouteSavedStateConfiguration, AppRoute.Home)
    val navigator = remember(backStack) { AppNavigator(backStack) }
    CompositionLocalProvider(LocalAppNavigator provides navigator) {
        NavDisplay(
            backStack = backStack,
            onBack = navigator::pop,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
                rememberResultEventBusNavEntryDecorator(),
            ),
            // as with Voyager, the screens aren't animated
            transitionSpec = { ContentTransform(EnterTransition.None, ExitTransition.None) },
            popTransitionSpec = { ContentTransform(EnterTransition.None, ExitTransition.None) },
            predictivePopTransitionSpec = { ContentTransform(EnterTransition.None, ExitTransition.None) },
            entryProvider = entryProvider {
                entry<AppRoute.Home> { HomeScreen() }
                entry<AppRoute.Pokedex> { PokedexScreen() }
                entry<AppRoute.CloudSync> { CloudSyncScreen() }
                entry<AppRoute.ManualReplay> { ManualReplayScreen(it.teamId, it.replayReference) }
                entry<AppRoute.Team> { TeamSpikeScreen(it.teamId) }
                entry<AppRoute.NotMigrated> { NotMigratedSpikeScreen(it.name) }
            },
        )
    }
}
