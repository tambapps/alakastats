package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.error.GetPokemonDataError
import com.tambapps.pokemon.alakastats.domain.repository.PokemonBaseStatsRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PokedexViewModelTest {

    private val metagross = PokemonName("metagross")
    private val megaMetagross = PokemonName("metagross-mega")
    private val stats = PokeStats(80, 135, 130, 95, 90, 70)

    private var loads = 0
    private var failure: GetPokemonDataError? = null

    private val repository = object : PokemonBaseStatsRepository {
        override suspend fun getBaseStats(pokemons: List<PokemonName>): Either<GetPokemonDataError, Map<PokemonName, PokeStats>> {
            loads++
            failure?.let { return it.left() }
            // zeros for what isn't known, as the real one
            return pokemons.associate { it.normalized to if (it == metagross || it == megaMetagross) stats else PokeStats(0, 0, 0, 0, 0, 0) }.right()
        }
    }

    private val viewModel = PokedexViewModel(repository)

    @Test
    fun thePokemonComesWithItsMegas() = runTest {
        viewModel.show(metagross)
        assertEquals(metagross, viewModel.selectedForm)
        assertEquals(listOf(metagross, megaMetagross), viewModel.forms)
        assertEquals(stats, viewModel.baseStats)
    }

    @Test
    fun aMegaOpensOnItsPokemonsForms() = runTest {
        viewModel.show(megaMetagross)
        assertEquals(megaMetagross, viewModel.selectedForm)
        assertEquals(listOf(metagross, megaMetagross), viewModel.forms)
        assertNull(viewModel.formToShowInstead)
    }

    @Test
    fun changingOfFormDoesntLoadAgain() = runTest {
        viewModel.show(metagross)
        viewModel.show(megaMetagross)
        assertEquals(1, loads)
        assertEquals(megaMetagross, viewModel.selectedForm)
    }

    @Test
    fun aMegaThatDoesntExistGoesToItsPokemon() = runTest {
        viewModel.show(PokemonName("metagross-mega-x"))
        assertEquals(metagross, viewModel.formToShowInstead)
    }

    @Test
    fun noFormShowsNothing() = runTest {
        viewModel.show(metagross)
        viewModel.show(null)
        assertNull(viewModel.selectedForm)
        assertEquals(emptyList(), viewModel.forms)
    }

    @Test
    fun aFailureIsntAnUnknownForm() = runTest {
        failure = GetPokemonDataError("offline")
        viewModel.show(megaMetagross)
        assertEquals(megaMetagross, viewModel.selectedForm)
        assertNull(viewModel.formToShowInstead)
        assertNull(viewModel.baseStats)
    }

    @Test
    fun theBaseFormOfAMegaIsItsPokemon() {
        assertEquals(metagross, baseFormOf(megaMetagross))
        assertEquals(PokemonName("charizard"), baseFormOf(PokemonName("charizard-mega-x")))
        assertEquals(metagross, baseFormOf(metagross))
        assertEquals(PokemonName("ogerpon-wellspring"), baseFormOf(PokemonName("ogerpon-wellspring")))
    }
}
