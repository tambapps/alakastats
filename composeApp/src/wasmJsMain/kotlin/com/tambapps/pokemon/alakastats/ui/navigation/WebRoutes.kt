package com.tambapps.pokemon.alakastats.ui.navigation

import androidx.navigation3.runtime.NavKey

private const val POKEDEX_PATH = "pokedex"

/**
 * The URLs of the web app, as paths relative to its root
 */
internal object WebRoutes {

    /**
     * The screens to open for a path. The home screen is always below, for the back button of the screens to work.
     * A path that isn't known opens the home screen
     */
    fun backStackOf(path: String): List<AppRoute> {
        val segments = path.split('/').filter { it.isNotEmpty() }
        val route = when {
            segments.singleOrNull() == POKEDEX_PATH -> AppRoute.Pokedex()
            segments.size == 2 && segments[0] == POKEDEX_PATH -> AppRoute.Pokedex(segments[1].lowercase())
            else -> return listOf(AppRoute.Home)
        }
        return listOf(AppRoute.Home, route)
    }

    /**
     * The path of a back stack: the one of its last screen having one, as not all screens have an URL
     */
    fun pathOf(backStack: List<NavKey>): String =
        backStack.asReversed().firstNotNullOfOrNull(::pathOfRoute).orEmpty()

    private fun pathOfRoute(route: NavKey): String? = when (route) {
        AppRoute.Home -> ""
        is AppRoute.Pokedex -> if (route.form != null) "$POKEDEX_PATH/${route.form}" else POKEDEX_PATH
        else -> null
    }
}
