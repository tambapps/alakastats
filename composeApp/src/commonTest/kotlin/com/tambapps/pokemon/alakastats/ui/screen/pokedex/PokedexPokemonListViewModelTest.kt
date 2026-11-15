package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import com.tambapps.pokemon.alakastats.infrastructure.repository.LocalPokedexRepository
import com.tambapps.pokemon.alakastats.ui.model.PokedexPokemonSort
import com.tambapps.pokemon.alakastats.ui.model.PokedexPokemonSortCriterion
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The list of the Pokémon, on the bundled Pokédex */
class PokedexPokemonListViewModelTest {

    private val viewModel = PokedexPokemonListViewModel(LocalPokedexRepository(Json { ignoreUnknownKeys = true }))

    private fun ids() = viewModel.pokemons.map { it.id }

    @Test
    fun everyPokemonButTheMegasIsListedByName() = runTest {
        viewModel.load()
        assertFalse(viewModel.isLoading)
        val ids = ids()
        assertTrue("charizard" in ids)
        assertFalse(viewModel.pokemons.any { it.isMega })
        assertEquals(viewModel.pokemons.sortedBy { it.name.en }, viewModel.pokemons)
    }

    @Test
    fun theMegasCanBeIncluded() = runTest {
        viewModel.load()
        val withoutMegas = viewModel.pokemons.size
        viewModel.includeMegas = true
        assertTrue("charizard-mega-y" in ids())
        assertEquals(withoutMegas + viewModel.pokemons.count { it.isMega }, viewModel.pokemons.size)
        viewModel.query = "Dracaufeu"
        assertEquals(listOf("charizard", "charizard-mega-x", "charizard-mega-y"), ids())
    }

    @Test
    fun theOrderCanBeReversed() = runTest {
        viewModel.load()
        viewModel.sort = PokedexPokemonSort(PokedexPokemonSortCriterion.NAME, descending = true)
        assertEquals(viewModel.pokemons.sortedByDescending { it.name.en }, viewModel.pokemons)
    }

    @Test
    fun thePokemonCanBeSortedBySpeedTheFastestFirst() = runTest {
        viewModel.load()
        viewModel.sort = PokedexPokemonSort(PokedexPokemonSortCriterion.SPEED)
        assertTrue(viewModel.sort.descending)
        val speeds = viewModel.pokemons.map { it.baseStats.speed }
        assertEquals(speeds.sortedDescending(), speeds)
        // the ones of the same speed by name
        val sameSpeed = viewModel.pokemons.filter { it.baseStats.speed == speeds.first() }
        assertEquals(sameSpeed.sortedBy { it.name.en }, sameSpeed)

        viewModel.sort = PokedexPokemonSort(PokedexPokemonSortCriterion.SPEED, descending = false)
        val slowestFirst = viewModel.pokemons.map { it.baseStats.speed }
        assertEquals(slowestFirst.sorted(), slowestFirst)
    }

    @Test
    fun aPokemonIsSearchedByItsEnglishOrFrenchName() = runTest {
        viewModel.load()
        viewModel.query = "charizard"
        assertEquals(listOf("charizard"), ids())
        viewModel.query = "Dracaufeu"
        assertEquals(listOf("charizard"), ids())
        // a part of the name, without its accents
        viewModel.query = "ekai"
        assertEquals(listOf("kommo-o"), ids())
    }

    @Test
    fun aTypoStillFindsThePokemon() = runTest {
        viewModel.load()
        viewModel.query = "garchmop"
        assertEquals("garchomp", ids().first())
    }
}
