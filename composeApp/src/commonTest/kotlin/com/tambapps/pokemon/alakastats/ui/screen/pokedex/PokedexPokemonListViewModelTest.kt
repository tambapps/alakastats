package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import com.tambapps.pokemon.alakastats.infrastructure.repository.LocalPokedexRepository
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
    fun theOrderCanBeReversed() = runTest {
        viewModel.load()
        viewModel.toggleOrder()
        assertEquals(viewModel.pokemons.sortedByDescending { it.name.en }, viewModel.pokemons)
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
