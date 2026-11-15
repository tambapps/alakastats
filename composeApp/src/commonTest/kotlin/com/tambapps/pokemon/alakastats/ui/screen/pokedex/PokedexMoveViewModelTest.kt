package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.alakastats.infrastructure.repository.LocalPokedexRepository
import com.tambapps.pokemon.alakastats.ui.model.PokedexPokemonSort
import com.tambapps.pokemon.alakastats.ui.model.PokedexPokemonSortCriterion
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The sheet of a move, on the bundled Pokédex */
class PokedexMoveViewModelTest {

    private val viewModel = PokedexMoveViewModel(LocalPokedexRepository(Json { ignoreUnknownKeys = true }))

    private fun ids() = viewModel.pokemons.map { it.id }

    @Test
    fun theMoveComesWithThePokemonLearningIt() = runTest {
        viewModel.load("trick-room")
        assertEquals("Trick Room", viewModel.move?.name?.en)
        assertFalse(viewModel.isUnknown)
        assertTrue("slowbro" in ids())
        assertFalse("incineroar" in ids())
        // without the Megas, by name
        assertFalse(viewModel.pokemons.any { it.isMega })
        assertEquals(viewModel.pokemons.size, viewModel.learnerCount)
        assertEquals(viewModel.pokemons.sortedBy { it.name.en }, viewModel.pokemons)
    }

    @Test
    fun theMegasCanBeIncluded() = runTest {
        viewModel.load("trick-room")
        viewModel.includeMegas = true
        assertTrue("slowbro-mega" in ids())
        // the count is the one without the Megas
        assertTrue(viewModel.pokemons.size > viewModel.learnerCount)
    }

    @Test
    fun onlyThePokemonOfTheMovesTypeCanBeShown() = runTest {
        viewModel.load("earthquake")
        viewModel.stabOnly = true
        assertTrue("garchomp" in ids())
        assertTrue(viewModel.pokemons.all { PokeType.GROUND in it.types && viewModel.isStab(it) })
    }

    @Test
    fun thePokemonCanBeSortedBySpeed() = runTest {
        viewModel.load("trick-room")
        // the slowest first, for Trick Room
        viewModel.sort = PokedexPokemonSort(PokedexPokemonSortCriterion.SPEED, descending = false)
        val speeds = viewModel.pokemons.map { it.baseStats.speed }
        assertEquals(speeds.sorted(), speeds)
    }

    @Test
    fun aMoveWrittenOtherwiseIsFound() = runTest {
        viewModel.load("King's Shield")
        assertEquals("king's-shield", viewModel.move?.id)
    }

    @Test
    fun aMoveNotInThePokedexIsUnknown() = runTest {
        viewModel.load("not-a-move")
        assertTrue(viewModel.isUnknown)
        assertNull(viewModel.move)
        assertEquals(emptyList(), viewModel.pokemons)
    }
}
