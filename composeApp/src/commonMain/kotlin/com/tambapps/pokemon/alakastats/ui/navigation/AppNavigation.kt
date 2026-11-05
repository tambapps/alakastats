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
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.ui.screen.about.AboutScreen
import com.tambapps.pokemon.alakastats.ui.screen.cloudsync.CloudSyncScreen
import com.tambapps.pokemon.alakastats.ui.screen.damagecalc.DamageCalcScreen
import com.tambapps.pokemon.alakastats.ui.screen.editteam.EditTeamScreen
import com.tambapps.pokemon.alakastats.ui.screen.home.HomeScreen
import com.tambapps.pokemon.alakastats.ui.screen.manualreplay.ManualReplayScreen
import com.tambapps.pokemon.alakastats.ui.screen.pokedex.PokedexScreen
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.NatureQuizScreen
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.NatureQuizSetupScreen
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.QuizDirection
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.QuizzesHomeScreen
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.SpeedInteractionsHomeSetupScreen
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.SpeedInteractionsMode
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.SpeedInteractionsQuizScreen
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.SpeedInteractionsTeamSetupScreen
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.SpeedStatQuizScreen
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.SpeedStatQuizSetupScreen
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.TeamlyticsScreen
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail.PokemonDetailsScreen
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclassesOfSealed
import kotlin.uuid.Uuid

/**
 * The app's screens. They're saved with the back stack (e.g. when the Android activity is recreated): they must only
 * hold what identifies what they show, not the data itself
 */
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

    /** The damage calc alone, on the web app's damage-calc page */
    @Serializable
    data object StandaloneDamageCalc : AppRoute

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
fun AppNavDisplay(startRoute: AppRoute) {
    val backStack = rememberNavBackStack(appRouteSavedStateConfiguration, startRoute)
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
            // the screens aren't animated
            transitionSpec = { ContentTransform(EnterTransition.None, ExitTransition.None) },
            popTransitionSpec = { ContentTransform(EnterTransition.None, ExitTransition.None) },
            predictivePopTransitionSpec = { ContentTransform(EnterTransition.None, ExitTransition.None) },
            entryProvider = entryProvider {
                entry<AppRoute.Home> { HomeScreen() }
                entry<AppRoute.Pokedex> { PokedexScreen() }
                entry<AppRoute.CloudSync> { CloudSyncScreen() }
                entry<AppRoute.About> { AboutScreen() }
                entry<AppRoute.DamageCalc> { DamageCalcScreen(showBackButton = true) }
                entry<AppRoute.StandaloneDamageCalc> { DamageCalcScreen(showBackButton = false) }
                entry<AppRoute.Teamlytics> { TeamlyticsScreen(it.teamId) }
                entry<AppRoute.EditTeam> { EditTeamScreen(it.teamId, it.redirectToTeamlytics) }
                entry<AppRoute.PokemonDetails> { PokemonDetailsScreen(it.teamId, PokemonName(it.pokemonName)) }
                entry<AppRoute.ManualReplay> { ManualReplayScreen(it.teamId, it.replayReference) }
                entry<AppRoute.Quizzes> { QuizzesHomeScreen() }
                entry<AppRoute.NatureQuizSetup> { NatureQuizSetupScreen() }
                entry<AppRoute.NatureQuiz> { NatureQuizScreen(it.natures, it.direction) }
                entry<AppRoute.SpeedStatQuizSetup> { SpeedStatQuizSetupScreen() }
                entry<AppRoute.SpeedStatQuiz> {
                    SpeedStatQuizScreen(it.format, it.allowChoiceScarf, it.useMaxStatPoints, it.useSpeedBoostingNature)
                }
                entry<AppRoute.SpeedInteractionsHomeSetup> { SpeedInteractionsHomeSetupScreen() }
                entry<AppRoute.SpeedInteractionsTeamSetup> { SpeedInteractionsTeamSetupScreen(it.teamId) }
                entry<AppRoute.SpeedInteractionsQuiz> { SpeedInteractionsQuizScreen(it.mode) }
            },
        )
    }
}
