package com.tambapps.pokemon.alakastats.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun anUnknownPathOpensTheHomeScreen() {
        assertEquals(listOf(AppRoute.Home), WebRoutes.backStackOf("unknown"))
        assertEquals(listOf(AppRoute.Home), WebRoutes.backStackOf("pokedex/metagross/more"))
        assertEquals(listOf(AppRoute.Home), WebRoutes.backStackOf("index.html"))
    }

    @Test
    fun theLastScreenHavingAnUrlGivesThePath() {
        assertEquals("", WebRoutes.pathOf(listOf(AppRoute.Home)))
        assertEquals("pokedex", WebRoutes.pathOf(listOf(AppRoute.Home, AppRoute.Pokedex())))
        assertEquals("pokedex/metagross", WebRoutes.pathOf(listOf(AppRoute.Home, AppRoute.Pokedex("metagross"))))
        // no URL for the quizzes yet
        assertEquals("", WebRoutes.pathOf(listOf(AppRoute.Home, AppRoute.Quizzes)))
    }
}
