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
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.tambapps.pokemon.alakastats.ui.screen.cloudsync.CloudSyncScreen
import com.tambapps.pokemon.alakastats.ui.screen.home.HomeScreen
import com.tambapps.pokemon.alakastats.ui.screen.pokedex.PokedexScreen
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.uuid.Uuid

@Serializable
sealed interface AppRoute : NavKey {
    @Serializable
    data object Home : AppRoute

    @Serializable
    data object Pokedex : AppRoute

    @Serializable
    data object CloudSync : AppRoute

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

val appRouteSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(AppRoute.Home::class, AppRoute.Home.serializer())
            subclass(AppRoute.Pokedex::class, AppRoute.Pokedex.serializer())
            subclass(AppRoute.CloudSync::class, AppRoute.CloudSync.serializer())
            subclass(AppRoute.Team::class, AppRoute.Team.serializer())
            subclass(AppRoute.NotMigrated::class, AppRoute.NotMigrated.serializer())
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
            ),
            // as with Voyager, the screens aren't animated
            transitionSpec = { ContentTransform(EnterTransition.None, ExitTransition.None) },
            popTransitionSpec = { ContentTransform(EnterTransition.None, ExitTransition.None) },
            predictivePopTransitionSpec = { ContentTransform(EnterTransition.None, ExitTransition.None) },
            entryProvider = entryProvider {
                entry<AppRoute.Home> { HomeScreen() }
                entry<AppRoute.Pokedex> { PokedexScreen() }
                entry<AppRoute.CloudSync> { CloudSyncScreen() }
                entry<AppRoute.Team> { TeamSpikeScreen(it.teamId) }
                entry<AppRoute.NotMigrated> { NotMigratedSpikeScreen(it.name) }
            },
        )
    }
}
