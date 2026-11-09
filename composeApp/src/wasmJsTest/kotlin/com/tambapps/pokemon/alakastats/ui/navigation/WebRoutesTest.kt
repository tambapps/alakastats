package com.tambapps.pokemon.alakastats.ui.navigation

import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.SpeedInteractionsMode
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.TeamlyticsTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class WebRoutesTest {

    @Test
    fun theRootOpensTheHomeScreen() {
        assertEquals(listOf(AppRoute.Home), WebRoutes.backStackOf(""))
    }

    @Test
    fun thePokedexIsAboveTheHomeScreen() {
        assertEquals(listOf(AppRoute.Home, AppRoute.Pokedex()), WebRoutes.backStackOf("pokedex"))
    }

    @Test
    fun aPokemonIsInThePokedex() {
        assertEquals(listOf(AppRoute.Home, AppRoute.Pokedex("metagross")), WebRoutes.backStackOf("pokedex/metagross"))
        assertEquals(listOf(AppRoute.Home, AppRoute.Pokedex("metagross-mega")), WebRoutes.backStackOf("pokedex/metagross-mega"))
    }

    @Test
    fun aFormStaysAsItIsWithoutBeingNormalized() {
        assertEquals(listOf(AppRoute.Home, AppRoute.Pokedex("ogerpon-wellspring")), WebRoutes.backStackOf("pokedex/ogerpon-wellspring"))
    }

    @Test
    fun thePathIsToleratedWithCapitalsAndATrailingSlash() {
        assertEquals(listOf(AppRoute.Home, AppRoute.Pokedex("metagross")), WebRoutes.backStackOf("pokedex/Metagross/"))
    }

    @Test
    fun theShowdownReplaysAreAboveTheHomeScreen() {
        assertEquals(listOf(AppRoute.Home, AppRoute.ShowdownReplays), WebRoutes.backStackOf("replays"))
    }

    @Test
    fun aboutIsAboveTheHomeScreen() {
        assertEquals(listOf(AppRoute.Home, AppRoute.About), WebRoutes.backStackOf("about"))
    }

    @Test
    fun theCloudSyncIsAboveTheHomeScreen() {
        assertEquals(listOf(AppRoute.Home, AppRoute.CloudSync), WebRoutes.backStackOf("cloud-sync"))
    }

    @Test
    fun theDamageCalcIsAboveTheHomeScreen() {
        assertEquals(listOf(AppRoute.Home, AppRoute.DamageCalc), WebRoutes.backStackOf("damage-calc"))
    }

    @Test
    fun theQuizzesAreAboveTheHomeScreenAndTheirSetupsAboveThem() {
        assertEquals(listOf(AppRoute.Home, AppRoute.Quizzes), WebRoutes.backStackOf("quizzes"))
        assertEquals(listOf(AppRoute.Home, AppRoute.Quizzes, AppRoute.NatureQuizSetup), WebRoutes.backStackOf("quizzes/natures"))
        assertEquals(listOf(AppRoute.Home, AppRoute.Quizzes, AppRoute.SpeedStatQuizSetup), WebRoutes.backStackOf("quizzes/speed-stat"))
        assertEquals(listOf(AppRoute.Home, AppRoute.Quizzes, AppRoute.SpeedInteractionsHomeSetup), WebRoutes.backStackOf("quizzes/speed-interactions"))
    }

    @Test
    fun anUnknownPathOpensTheHomeScreen() {
        assertEquals(listOf(AppRoute.Home), WebRoutes.backStackOf("unknown"))
        assertEquals(listOf(AppRoute.Home), WebRoutes.backStackOf("pokedex/metagross/more"))
        assertEquals(listOf(AppRoute.Home), WebRoutes.backStackOf("quizzes/unknown"))
        assertEquals(listOf(AppRoute.Home), WebRoutes.backStackOf("index.html"))
    }

    @Test
    fun eachFixedPathIsTheOneOfItsBackStack() {
        for (path in listOf("", "replays", "about", "cloud-sync", "damage-calc", "quizzes", "quizzes/natures", "quizzes/speed-stat", "quizzes/speed-interactions")) {
            assertEquals(path, WebRoutes.pathOf(WebRoutes.backStackOf(path)))
        }
    }

    @Test
    fun aRunningQuizIsAtTheUrlOfItsSetup() {
        val setup = WebRoutes.backStackOf("quizzes/speed-stat")
        val quiz = AppRoute.SpeedStatQuiz(Format.REGULATION_MC, false, false, false)
        assertEquals("quizzes/speed-stat", WebRoutes.pathOf(setup + quiz))
    }

    private val teamId = Uuid.parse("0190a6f4-1234-7abc-8def-0123456789ab")

    @Test
    fun aTeamIsAboveTheHomeScreenOnItsFirstTab() {
        assertEquals(listOf(AppRoute.Home, AppRoute.Teamlytics(teamId)), WebRoutes.backStackOf("teams/$teamId"))
    }

    @Test
    fun eachTabOfATeamHasItsUrl() {
        for (tab in TeamlyticsTab.entries) {
            val backStack = WebRoutes.backStackOf("teams/$teamId/${tab.path}")
            assertEquals(listOf(AppRoute.Home, AppRoute.Teamlytics(teamId, tab)), backStack)
        }
    }

    @Test
    fun theFirstTabIsTheTeamsOwnUrl() {
        assertEquals("teams/$teamId", WebRoutes.pathOf(listOf(AppRoute.Home, AppRoute.Teamlytics(teamId))))
        assertEquals(
            listOf(AppRoute.Home, AppRoute.Teamlytics(teamId)),
            WebRoutes.backStackOf("teams/$teamId/overview"),
        )
    }

    @Test
    fun theOtherTabsAreInTheUrl() {
        val backStack = listOf(AppRoute.Home, AppRoute.Teamlytics(teamId, TeamlyticsTab.GAME_PLANS))
        assertEquals("teams/$teamId/game-plans", WebRoutes.pathOf(backStack))
    }

    @Test
    fun aReplayCanBeAskedOnTheReplaysTab() {
        val backStack = WebRoutes.backStackOf("teams/$teamId/replays?replayRef=gen9vgc2026-123")
        assertEquals(listOf(AppRoute.Home, AppRoute.Teamlytics(teamId, TeamlyticsTab.REPLAYS, "gen9vgc2026-123")), backStack)
        assertEquals("teams/$teamId/replays?replayRef=gen9vgc2026-123", WebRoutes.pathOf(backStack))
    }

    @Test
    fun theReferenceOfAReplayKeepsItsCaseAndIsEncoded() {
        val route = AppRoute.Teamlytics(teamId, TeamlyticsTab.REPLAYS, "Gen9 VGC&x=1")
        val path = WebRoutes.pathOf(listOf(AppRoute.Home, route))
        assertEquals(false, ' ' in path || '&' in path)
        assertEquals(listOf(AppRoute.Home, route), WebRoutes.backStackOf(path))
    }

    @Test
    fun theQueryIsOptionalAndIgnoredOnTheOtherTabs() {
        val onReplays = listOf(AppRoute.Home, AppRoute.Teamlytics(teamId, TeamlyticsTab.REPLAYS))
        assertEquals(onReplays, WebRoutes.backStackOf("teams/$teamId/replays"))
        assertEquals(onReplays, WebRoutes.backStackOf("teams/$teamId/replays?replayRef="))
        assertEquals(onReplays, WebRoutes.backStackOf("teams/$teamId/replays?other=1"))
        assertEquals(
            listOf(AppRoute.Home, AppRoute.Teamlytics(teamId, TeamlyticsTab.USAGES)),
            WebRoutes.backStackOf("teams/$teamId/usages?replayRef=abc"),
        )
    }

    @Test
    fun anUnknownTabOpensTheTeam() {
        assertEquals(listOf(AppRoute.Home, AppRoute.Teamlytics(teamId)), WebRoutes.backStackOf("teams/$teamId/unknown"))
    }

    @Test
    fun anInvalidTeamIdOpensTheHomeScreen() {
        assertEquals(listOf(AppRoute.Home), WebRoutes.backStackOf("teams/not-an-id"))
        assertEquals(listOf(AppRoute.Home), WebRoutes.backStackOf("teams"))
        assertEquals(listOf(AppRoute.Home), WebRoutes.backStackOf("teams/$teamId/replays/more"))
    }

    @Test
    fun aPokemonOfATeamIsAboveTheTeamsFirstTab() {
        val backStack = listOf(AppRoute.Home, AppRoute.Teamlytics(teamId), AppRoute.PokemonDetails(teamId, "metagross-mega"))
        assertEquals(backStack, WebRoutes.backStackOf("teams/$teamId/pokemon/metagross-mega"))
        assertEquals("teams/$teamId/pokemon/metagross-mega", WebRoutes.pathOf(backStack))
    }

    @Test
    fun aPokemonOfATeamIsNormalizedAsTheAppDoes() {
        val expected = listOf(AppRoute.Home, AppRoute.Teamlytics(teamId), AppRoute.PokemonDetails(teamId, "iron-hands"))
        assertEquals(expected, WebRoutes.backStackOf("teams/$teamId/pokemon/Iron%20Hands"))
    }

    @Test
    fun aPokemonOfATeamWithANameThatIsntUrlSafeIsEncoded() {
        for (name in listOf("flabébé", "mr.-mime", "type:-null", "farfetch’d")) {
            val backStack = listOf(AppRoute.Home, AppRoute.Teamlytics(teamId), AppRoute.PokemonDetails(teamId, name))
            assertEquals(backStack, WebRoutes.backStackOf(WebRoutes.pathOf(backStack)))
        }
        assertEquals(
            "teams/$teamId/pokemon/flab%C3%A9b%C3%A9",
            WebRoutes.pathOf(listOf(AppRoute.PokemonDetails(teamId, "flabébé"))),
        )
    }

    @Test
    fun aBadlyEncodedPokemonOpensTheHomeScreen() {
        assertEquals(listOf(AppRoute.Home), WebRoutes.backStackOf("teams/$teamId/pokemon/%zz"))
    }

    @Test
    fun theQuizOfATeamIsAboveItsQuizzesTab() {
        val backStack = listOf(AppRoute.Home, AppRoute.Teamlytics(teamId, TeamlyticsTab.QUIZZES), AppRoute.SpeedInteractionsTeamSetup(teamId))
        assertEquals(backStack, WebRoutes.backStackOf("teams/$teamId/quizzes/speed-interactions"))
        assertEquals("teams/$teamId/quizzes/speed-interactions", WebRoutes.pathOf(backStack))
    }

    @Test
    fun aRunningQuizOfATeamIsAtTheUrlOfItsSetup() {
        val backStack = listOf(
            AppRoute.Home,
            AppRoute.Teamlytics(teamId, TeamlyticsTab.QUIZZES),
            AppRoute.SpeedInteractionsTeamSetup(teamId),
            AppRoute.SpeedInteractionsQuiz(SpeedInteractionsMode.Team(teamId)),
        )
        assertEquals("teams/$teamId/quizzes/speed-interactions", WebRoutes.pathOf(backStack))
    }

    @Test
    fun anUnknownScreenOfATeamOpensTheHomeScreen() {
        assertEquals(listOf(AppRoute.Home), WebRoutes.backStackOf("teams/$teamId/quizzes/unknown"))
        assertEquals(listOf(AppRoute.Home), WebRoutes.backStackOf("teams/$teamId/usages/speed-interactions"))
        assertEquals(listOf(AppRoute.Home), WebRoutes.backStackOf("teams/not-an-id/pokemon/metagross"))
        assertEquals(listOf(AppRoute.Home), WebRoutes.backStackOf("teams/$teamId/pokemon/metagross/more"))
    }

    @Test
    fun aTeamsScreensWithoutUrlAreAtTheUrlOfItsTab() {
        val backStack = listOf(AppRoute.Home, AppRoute.Teamlytics(teamId, TeamlyticsTab.REPLAYS), AppRoute.ManualReplay(teamId))
        assertEquals("teams/$teamId/replays", WebRoutes.pathOf(backStack))
    }

    @Test
    fun theLastScreenHavingAnUrlGivesThePath() {
        assertEquals("", WebRoutes.pathOf(listOf(AppRoute.Home)))
        assertEquals("pokedex", WebRoutes.pathOf(listOf(AppRoute.Home, AppRoute.Pokedex())))
        assertEquals("pokedex/metagross", WebRoutes.pathOf(listOf(AppRoute.Home, AppRoute.Pokedex("metagross"))))
        // no URL for this screen yet
        assertEquals("", WebRoutes.pathOf(listOf(AppRoute.Home, AppRoute.EditTeam())))
    }
}
