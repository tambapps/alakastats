package com.tambapps.pokemon.alakastats.ui.navigation

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.calculate
import alakastats.composeapp.generated.resources.group
import alakastats.composeapp.generated.resources.menu
import alakastats.composeapp.generated.resources.play_arrow
import alakastats.composeapp.generated.resources.pokedex
import alakastats.composeapp.generated.resources.quiz
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * The parts of the app, reached from the navigation drawer
 */
internal enum class TopLevelDestination(val label: String, val icon: DrawableResource, val route: AppRoute) {
    TEAMS("Teams", Res.drawable.group, AppRoute.Home),
    SHOWDOWN_REPLAYS("Replays", Res.drawable.play_arrow, AppRoute.ShowdownReplays),
    DAMAGE_CALC("Damage Calc", Res.drawable.calculate, AppRoute.DamageCalc),
    QUIZZES("Quizzes", Res.drawable.quiz, AppRoute.Quizzes),
    POKEDEX("Pokédex", Res.drawable.pokedex, AppRoute.Pokedex());

    companion object {
        /** The destination a screen is, null for a screen below one of them */
        fun of(route: NavKey?): TopLevelDestination? = when (route) {
            // whatever the form shown
            is AppRoute.Pokedex -> POKEDEX
            else -> entries.find { it.route == route }
        }
    }
}

/**
 * The navigation between the parts of the app: a drawer, closed until the menu button opens it for it not to take any
 * room. The button is only on the parts of the app, where the screens below them have their back button
 */
@Composable
internal fun WithAppNavigationDrawer(backStack: List<NavKey>, navigator: AppNavigator, content: @Composable () -> Unit) {
    val current = TopLevelDestination.of(backStack.lastOrNull())
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    // a screen without the drawer can't have it open
    LaunchedEffect(current == null) {
        if (current == null) drawerState.snapTo(DrawerValue.Closed)
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        // only to close it: opening it by dragging would be unexpected with a mouse, and would fight Android's back
        // gesture on phones
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(Modifier.width(280.dp)) {
                Text(
                    "Alakastats",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                )
                for (destination in TopLevelDestination.entries) {
                    NavigationDrawerItem(
                        selected = destination == current,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (destination != current) navigator.navigateToTopLevel(destination.route)
                        },
                        icon = { DestinationIcon(destination) },
                        label = { Text(destination.label) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            }
        },
    ) {
        // the content is always at the same place, whether the button is shown or not: it keeps the screens' state
        Box(Modifier.fillMaxSize()) {
            content()
            if (current != null) {
                // where the screens below the parts have their back button, out of the status bar and the notch
                IconButton(
                    onClick = { scope.launch { drawerState.open() } },
                    modifier = Modifier.align(Alignment.TopStart)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Start))
                        .padding(if (LocalIsCompact.current) 4.dp else 16.dp),
                ) {
                    Icon(painter = painterResource(Res.drawable.menu), contentDescription = "Menu")
                }
            }
        }
    }
}

@Composable
private fun DestinationIcon(destination: TopLevelDestination) {
    Icon(painter = painterResource(destination.icon), contentDescription = null, modifier = Modifier.size(24.dp))
}
