package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import arrow.core.Either
import arrow.core.left
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.error.GetPokemonDataError
import com.tambapps.pokemon.alakastats.domain.model.DamageClass
import com.tambapps.pokemon.alakastats.domain.model.Pokedex
import com.tambapps.pokemon.alakastats.domain.repository.PokedexRepository
import com.tambapps.pokemon.alakastats.infrastructure.repository.LocalPokedexRepository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The sheet of a Pokémon, on the bundled Pokédex */
class PokedexViewModelTest {

    private val metagross = PokemonName("metagross")
    private val megaMetagross = PokemonName("metagross-mega")

    private var loads = 0
    private var failure: GetPokemonDataError? = null

    private val repository = object : PokedexRepository {
        private val bundled = LocalPokedexRepository(Json { ignoreUnknownKeys = true })

        override suspend fun getPokedex(): Either<GetPokemonDataError, Pokedex> {
            loads++
            failure?.let { return it.left() }
            return bundled.getPokedex()
        }
    }

    private val viewModel = PokedexViewModel(repository)

    @Test
    fun thePokemonComesWithItsMegas() = runTest {
        viewModel.show(metagross)
        assertEquals(metagross, viewModel.selectedForm)
        assertEquals(listOf(metagross, megaMetagross), viewModel.forms)
        assertEquals(PokeStats(80, 135, 130, 95, 90, 70), viewModel.baseStats)
    }

    @Test
    fun aMegaOpensOnItsPokemonsForms() = runTest {
        viewModel.show(megaMetagross)
        assertEquals(megaMetagross, viewModel.selectedForm)
        assertEquals(listOf(metagross, megaMetagross), viewModel.forms)
        assertNull(viewModel.formToShowInstead)
    }

    // Mega Floette is the Mega of Floette-Eternal, not of Floette
    @Test
    fun aMegaIsAFormOfThePokemonItMegaEvolvesFrom() = runTest {
        val forms = listOf(PokemonName("floette-eternal"), PokemonName("floette-mega"))
        viewModel.show(PokemonName("floette-mega"))
        assertEquals(forms, viewModel.forms)
        viewModel.show(PokemonName("Floette-Eternal"))
        assertEquals(forms, viewModel.forms)
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
        assertFalse(viewModel.isUnknown)
    }

    @Test
    fun aPokemonNotInThePokedexIsUnknown() = runTest {
        viewModel.show(PokemonName("missingno"))
        assertTrue(viewModel.isUnknown)
        assertNull(viewModel.formToShowInstead)
    }

    @Test
    fun thePokemonComesWithItsAbilitiesAndMoves() = runTest {
        viewModel.show(PokemonName("Incineroar"))
        val species = viewModel.species!!
        assertEquals(listOf("blaze"), species.abilities.map { it.id })
        assertEquals("intimidate", species.hiddenAbility?.id)
        val ids = viewModel.moves.map { it.id }
        assertEquals(species.moves.size, ids.size)
        assertTrue("fake-out" in ids && "parting-shot" in ids)
        // by name
        assertEquals(viewModel.moves.sortedBy { it.name.en }, viewModel.moves)
    }

    @Test
    fun theMovesAreSearchedAndFilteredByCategory() = runTest {
        viewModel.show(PokemonName("Incineroar"))
        viewModel.moveQuery = "flare blitz"
        assertEquals(listOf("flare-blitz"), viewModel.moves.map { it.id })
        viewModel.moveQuery = ""
        viewModel.moveCategory = DamageClass.STATUS
        assertTrue(viewModel.moves.isNotEmpty())
        assertTrue(viewModel.moves.all { it.damageClass == DamageClass.STATUS })
    }

    @Test
    fun theMovesAreFilteredByType() = runTest {
        viewModel.show(PokemonName("Incineroar"))
        viewModel.moveType = PokeType.FIRE
        assertTrue("flare-blitz" in viewModel.moves.map { it.id })
        assertTrue(viewModel.moves.all { it.type == PokeType.FIRE })
        // with the category
        viewModel.moveCategory = DamageClass.STATUS
        assertTrue("will-o-wisp" in viewModel.moves.map { it.id })
        assertTrue(viewModel.moves.all { it.type == PokeType.FIRE && it.damageClass == DamageClass.STATUS })
        // the filter only proposes the types of its moves
        assertEquals(viewModel.species!!.moves.map { it.type }.toSet(), viewModel.moveTypes)
    }

    @Test
    fun aMegaHasTheMovesOfItsBaseForm() = runTest {
        viewModel.show(metagross)
        val moves = viewModel.moves
        viewModel.show(megaMetagross)
        assertEquals(moves, viewModel.moves)
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
        assertFalse(viewModel.isUnknown)
        assertNull(viewModel.baseStats)
    }
}
