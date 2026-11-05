package com.tambapps.pokemon.alakastats.ui.navigation

import androidx.navigation3.runtime.NavKey

private const val POKEDEX_PATH = "pokedex"

/**
 * The screens to open for each fixed path, the last one being shown. The screens below are there for the back button of
 * the screens to work. damage-calc is also a real page, showing the damage calc alone: it's what opens from its URL
 */
private val BACK_STACK_BY_PATH: Map<String, List<AppRoute>> = mapOf(
    "" to listOf(AppRoute.Home),
    "about" to listOf(AppRoute.Home, AppRoute.About),
    "damage-calc" to listOf(AppRoute.Home, AppRoute.DamageCalc),
    "quizzes" to listOf(AppRoute.Home, AppRoute.Quizzes),
    "quizzes/natures" to listOf(AppRoute.Home, AppRoute.Quizzes, AppRoute.NatureQuizSetup),
    "quizzes/speed-stat" to listOf(AppRoute.Home, AppRoute.Quizzes, AppRoute.SpeedStatQuizSetup),
    "quizzes/speed-interactions" to listOf(AppRoute.Home, AppRoute.Quizzes, AppRoute.SpeedInteractionsHomeSetup),
)

/**
 * The URLs of the web app, as paths relative to its root
 */
internal object WebRoutes {

    /**
     * The screens to open for a path. A path that isn't known opens the home screen
     */
    fun backStackOf(path: String): List<AppRoute> {
        val normalizedPath = path.split('/').filter { it.isNotEmpty() }.joinToString("/").lowercase()
        BACK_STACK_BY_PATH[normalizedPath]?.let { return it }
        val segments = normalizedPath.split('/')
        return when {
            segments.singleOrNull() == POKEDEX_PATH -> listOf(AppRoute.Home, AppRoute.Pokedex())
            segments.size == 2 && segments[0] == POKEDEX_PATH -> listOf(AppRoute.Home, AppRoute.Pokedex(segments[1]))
            else -> listOf(AppRoute.Home)
        }
    }

    /**
     * The path of a back stack: the one of its last screen having one, as not all screens have an URL (e.g. a running
     * quiz is at the URL of its setup)
     */
    fun pathOf(backStack: List<NavKey>): String =
        backStack.asReversed().firstNotNullOfOrNull(::pathOfRoute).orEmpty()

    private fun pathOfRoute(route: NavKey): String? = when (route) {
        is AppRoute.Pokedex -> if (route.form != null) "$POKEDEX_PATH/${route.form}" else POKEDEX_PATH
        else -> BACK_STACK_BY_PATH.entries.find { it.value.last() == route }?.key
    }
}
