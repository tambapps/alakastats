package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.alakastats.infrastructure.repository.LocalPokedexRepository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The list of the abilities and the sheet of one, on the bundled Pokédex */
class PokedexAbilitiesViewModelTest {

    private val repository = LocalPokedexRepository(Json { ignoreUnknownKeys = true })
    private val listViewModel = PokedexAbilitiesViewModel(repository)
    private val sheetViewModel = PokedexAbilityViewModel(repository)

    private fun listIds() = listViewModel.abilities.map { it.ability.id }
    private fun sheetIds() = sheetViewModel.pokemons.map { it.id }

    @Test
    fun everyAbilityIsListedByNameWithItsPokemon() = runTest {
        listViewModel.load()
        assertEquals(listViewModel.all.size, listViewModel.abilities.size)
        assertEquals(listViewModel.abilities.sortedBy { it.ability.name.en }, listViewModel.abilities)
        val intimidate = listViewModel.abilities.first { it.ability.id == "intimidate" }
        assertTrue(intimidate.pokemons.any { it.id == "incineroar" })
        assertFalse(intimidate.pokemons.any { it.isMega })
    }

    @Test
    fun anAbilityIsSearchedByNameOrDescription() = runTest {
        listViewModel.load()
        listViewModel.query = "Intimidation"
        assertEquals(listOf("intimidate"), listIds())
    }

    @Test
    fun theAbilitiesAreFiltered() = runTest {
        listViewModel.load()
        listViewModel.megaOnly = true
        assertTrue("mega-sol" in listIds())
        assertTrue(listViewModel.abilities.all { it.isMegaAbility })
    }

    @Test
    fun theAbilitiesCanBeSortedByTheirNumberOfPokemon() = runTest {
        listViewModel.load()
        listViewModel.sort = PokedexAbilitySort(PokedexAbilitySortCriterion.POKEMON)
        assertEquals("intimidate", listIds().first())
        val counts = listViewModel.abilities.map { it.shown.size }
        assertEquals(counts.sortedDescending(), counts)
    }

    @Test
    fun theSheetListsThePokemonHavingTheAbility() = runTest {
        sheetViewModel.load("drought")
        assertEquals("Drought", sheetViewModel.holders?.ability?.name?.en)
        assertEquals(listOf("ninetales", "torkoal"), sheetIds())
        sheetViewModel.includeMegas = true
        assertTrue("charizard-mega-y" in sheetIds())
        sheetViewModel.type = PokeType.FLYING
        assertEquals(listOf("charizard-mega-y"), sheetIds())
    }

    @Test
    fun theSheetOfAnAbilityOfMegasListsItsMegas() = runTest {
        sheetViewModel.load("Mega Sol")
        assertEquals(listOf("meganium-mega"), sheetIds())
    }

    @Test
    fun anAbilityNotInThePokedexIsUnknown() = runTest {
        sheetViewModel.load("not-an-ability")
        assertTrue(sheetViewModel.isUnknown)
        assertNull(sheetViewModel.holders)
    }
}
