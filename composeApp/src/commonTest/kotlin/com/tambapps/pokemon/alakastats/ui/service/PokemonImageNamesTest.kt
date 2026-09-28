package com.tambapps.pokemon.alakastats.ui.service

import com.tambapps.pokemon.PokemonName
import kotlin.test.Test
import kotlin.test.assertEquals

class PokemonImageNamesTest {

    @Test
    fun usesTheNormalizedName() {
        assertEquals("incineroar", PokemonName("Incineroar").imageName)
        assertEquals("rotom-wash", PokemonName("Rotom-Wash").imageName)
        assertEquals("mr.-rime", PokemonName("Mr. Rime").imageName)
    }

    @Test
    fun reordersChampionsMegaNames() {
        assertEquals("venusaur-mega", PokemonName("Mega Venusaur").imageName)
        assertEquals("charizard-mega-x", PokemonName("Mega Charizard X").imageName)
        assertEquals("absol-mega-z", PokemonName("Mega Absol Z").imageName)
    }

    @Test
    fun keepsShowdownMegaNames() {
        assertEquals("charizard-mega-x", PokemonName("Charizard-Mega-X").imageName)
    }

    @Test
    fun usesTheBaseFormImageForFormsWithoutImage() {
        assertEquals("aegislash", PokemonName("Aegislash-Shield").imageName)
        assertEquals("aegislash-blade", PokemonName("Aegislash-Blade").imageName)
        assertEquals("gourgeist", PokemonName("Gourgeist-Super").imageName)
    }
}
