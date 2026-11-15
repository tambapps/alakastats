package com.tambapps.pokemon.alakastats.domain.model

import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.Stat
import com.tambapps.pokemon.alakastats.infrastructure.repository.LocalPokedexRepository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The queries of the Pokédex, on the bundled one */
class PokedexTest {

    private val repository = LocalPokedexRepository(Json { ignoreUnknownKeys = true })

    private suspend fun pokedex(): Pokedex = repository.getPokedex().getOrNull()!!

    private fun List<PokedexEntry>.ids() = map { it.id }

    @Test
    fun theRepositoryLoadsOnce() = runTest {
        assertTrue(pokedex() === pokedex())
    }

    @Test
    fun thePokemonOfTwoTypes() = runTest {
        val pokedex = pokedex()
        assertEquals(listOf("swampert", "swampertmega"), pokedex.withTypes(PokeType.WATER, PokeType.GROUND).ids())
        assertTrue(pokedex.withTypes(PokeType.WATER).size > pokedex.withTypes(PokeType.WATER, PokeType.GROUND).size)
    }

    @Test
    fun thePokemonOfAnAbility() = runTest {
        val pokedex = pokedex()
        val intimidate = pokedex.withAbility(pokedex.ability("intimidate")!!)
        assertTrue("incineroar" in intimidate.ids())
        // a hidden ability too
        assertTrue(pokedex.withAbility(pokedex.ability("cudchew")!!).any { it.id == "taurospaldeaaqua" })
    }

    @Test
    fun theLearnersOfAMoveIncludeTheMegas() = runTest {
        val pokedex = pokedex()
        val learners = pokedex.learners(pokedex.move("flamethrower")!!).ids()
        assertTrue("charizard" in learners)
        assertTrue("charizardmegay" in learners)
    }

    @Test
    fun aCombinedSearch() = runTest {
        val pokedex = pokedex()
        // the Water Pokémon learning Trick Room, with a base speed below 50
        val filter = PokedexFilter(
            types = setOf(PokeType.WATER),
            learns = setOf(pokedex.move("trickroom")!!),
            stats = listOf(StatFilter(Stat.SPEED, max = 49)),
        )
        assertEquals(listOf("slowbro", "slowbromega", "slowking"), pokedex.search(filter).ids())
        assertEquals(listOf("slowbro", "slowking"), pokedex.search(filter.copy(includeMegas = false)).ids())
    }

    @Test
    fun aSearchSortedByStat() = runTest {
        val pokedex = pokedex()
        val fastest = pokedex.search(PokedexFilter(), sort = compareByDescending { it.baseStats.speed }).first()
        assertTrue(fastest.baseStats.speed >= pokedex.species.maxOf { it.baseStats.speed })
    }

    @Test
    fun theFormsOfAPokemon() = runTest {
        val pokedex = pokedex()
        val charizard = pokedex.species(PokemonName("Charizard"))!!
        val megaY = pokedex.species(PokemonName("charizard-mega-y"))!!
        val forms = listOf("charizard", "charizardmegax", "charizardmegay")
        assertEquals(forms, pokedex.formsOf(charizard).ids())
        assertEquals(forms, pokedex.formsOf(megaY).ids())
        assertEquals(megaY.moves, charizard.moves)
        assertNotNull(megaY.megaStone)
    }

    @Test
    fun theMovepoolIsSortedByName() = runTest {
        val pokedex = pokedex()
        val names = pokedex.movepool(pokedex.species("incineroar")!!).map { it.name.en }
        assertEquals(names.sorted(), names)
        assertTrue("Fake Out" in names)
    }

    @Test
    fun aSearchByEnglishOrFrenchName() = runTest {
        val pokedex = pokedex()
        assertEquals("charizard", pokedex.searchByName("Charizard").first().id)
        assertEquals("charizard", pokedex.searchByName("dracaufeu").first().id)
        assertEquals("trickroom", pokedex.searchByName("Distorsion").first().id)
    }

    @Test
    fun aSearchIgnoresAccentsCaseAndPunctuation() = runTest {
        val pokedex = pokedex()
        assertEquals("charizardmegay", pokedex.searchByName("mega dracaufeu y").first().id)
        assertEquals("charizardmegay", pokedex.searchByName("MÉGA-DRACAUFEU-Y").first().id)
        assertEquals("kommoo", pokedex.searchByName("kommo o").first().id)
        assertEquals("kommoo", pokedex.searchByName("ekaiser").first().id)
    }

    @Test
    fun aSearchToleratesAPrefixAndTypos() = runTest {
        val pokedex = pokedex()
        assertTrue("garchomp" in pokedex.searchByName("garch").ids())
        assertEquals("garchomp", pokedex.searchByName("garchmop").first().id)
        assertEquals("incineroar", pokedex.searchByName("incinerar").first().id)
        assertEquals(emptyList(), pokedex.searchByName("  "))
    }

    @Test
    fun aNameIsFolded() {
        assertEquals("megadracaufeuy", foldName("Méga-Dracaufeu Y"))
        assertEquals("farfetchd", foldName("Farfetch’d"))
        assertEquals("nidoranf", foldName("Nidoran♀"))
        assertEquals(1, typoDistance("garchmop", "garchomp"))
        assertEquals(2, typoDistance("abc", "a"))
    }
}
