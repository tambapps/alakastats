package com.tambapps.pokemon.alakastats.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.TeamlyticsTab
import io.ktor.http.encodeURLParameter
import io.ktor.http.parseQueryString
import kotlin.uuid.Uuid

private const val POKEDEX_PATH = "pokedex"
private const val TEAMS_PATH = "teams"
private const val REPLAY_REF_PARAM = "replayRef"

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
     * The screens to open for a path, with its query if it has one. A path that isn't known opens the home screen
     */
    fun backStackOf(url: String): List<AppRoute> {
        val path = url.substringBefore('?')
        val query = url.substringAfter('?', missingDelimiterValue = "")
        val normalizedPath = path.split('/').filter { it.isNotEmpty() }.joinToString("/").lowercase()
        BACK_STACK_BY_PATH[normalizedPath]?.let { return it }
        val segments = normalizedPath.split('/')
        return when {
            segments.singleOrNull() == POKEDEX_PATH -> listOf(AppRoute.Home, AppRoute.Pokedex())
            segments.size == 2 && segments[0] == POKEDEX_PATH -> listOf(AppRoute.Home, AppRoute.Pokedex(segments[1]))
            segments.size in 2..3 && segments[0] == TEAMS_PATH -> teamBackStackOf(segments[1], segments.getOrNull(2), query)
            else -> listOf(AppRoute.Home)
        }
    }

    /**
     * A tab that isn't known opens the team's first one. Whether the team exists is only known once it's loaded: its
     * screen goes back to the home screen if it doesn't
     */
    private fun teamBackStackOf(teamId: String, tabPath: String?, query: String): List<AppRoute> {
        val id = Uuid.parseOrNull(teamId) ?: return listOf(AppRoute.Home)
        val tab = TeamlyticsTab.entries.find { it.path == tabPath } ?: TeamlyticsTab.OVERVIEW
        // only the replays tab has replays to scroll to
        val replayRef = if (tab == TeamlyticsTab.REPLAYS) parseQueryString(query)[REPLAY_REF_PARAM]?.ifEmpty { null } else null
        return listOf(AppRoute.Home, AppRoute.Teamlytics(id, tab, replayRef))
    }

    /**
     * The path of a back stack: the one of its last screen having one, as not all screens have an URL (e.g. a running
     * quiz is at the URL of its setup)
     */
    fun pathOf(backStack: List<NavKey>): String =
        backStack.asReversed().firstNotNullOfOrNull(::pathOfRoute).orEmpty()

    private fun pathOfRoute(route: NavKey): String? = when (route) {
        is AppRoute.Pokedex -> if (route.form != null) "$POKEDEX_PATH/${route.form}" else POKEDEX_PATH
        is AppRoute.Teamlytics -> {
            val teamPath = "$TEAMS_PATH/${route.teamId}"
            val query = route.replayRef?.let { "?$REPLAY_REF_PARAM=${it.encodeURLParameter()}" }.orEmpty()
            // the first tab is the team's own URL
            if (route.tab == TeamlyticsTab.OVERVIEW) teamPath else "$teamPath/${route.tab.path}$query"
        }
        else -> BACK_STACK_BY_PATH.entries.find { it.value.last() == route }?.key
    }
}
