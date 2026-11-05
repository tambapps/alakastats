package com.tambapps.pokemon.alakastats.ui.navigation

import com.tambapps.pokemon.alakastats.domain.model.Format
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
    fun aboutIsAboveTheHomeScreen() {
        assertEquals(listOf(AppRoute.Home, AppRoute.About), WebRoutes.backStackOf("about"))
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
        for (path in listOf("", "about", "damage-calc", "quizzes", "quizzes/natures", "quizzes/speed-stat", "quizzes/speed-interactions")) {
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
    fun aTeamsScreensAreAtTheUrlOfItsTab() {
        val backStack = listOf(AppRoute.Home, AppRoute.Teamlytics(teamId, TeamlyticsTab.QUIZZES), AppRoute.SpeedInteractionsTeamSetup(teamId))
        assertEquals("teams/$teamId/quizzes", WebRoutes.pathOf(backStack))
    }

    @Test
    fun theLastScreenHavingAnUrlGivesThePath() {
        assertEquals("", WebRoutes.pathOf(listOf(AppRoute.Home)))
        assertEquals("pokedex", WebRoutes.pathOf(listOf(AppRoute.Home, AppRoute.Pokedex())))
        assertEquals("pokedex/metagross", WebRoutes.pathOf(listOf(AppRoute.Home, AppRoute.Pokedex("metagross"))))
        // no URL for this screen yet
        assertEquals("", WebRoutes.pathOf(listOf(AppRoute.Home, AppRoute.CloudSync)))
    }
}
