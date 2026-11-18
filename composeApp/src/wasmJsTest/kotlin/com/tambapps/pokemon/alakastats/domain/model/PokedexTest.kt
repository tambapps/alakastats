package com.tambapps.pokemon.alakastats.domain.model

import com.tambapps.pokemon.AbilityName
import com.tambapps.pokemon.ItemName
import com.tambapps.pokemon.MoveName
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
        assertEquals(listOf("swampert", "swampert-mega"), pokedex.withTypes(PokeType.WATER, PokeType.GROUND).ids())
        assertTrue(pokedex.withTypes(PokeType.WATER).size > pokedex.withTypes(PokeType.WATER, PokeType.GROUND).size)
    }

    @Test
    fun thePokemonOfAnAbility() = runTest {
        val pokedex = pokedex()
        val intimidate = pokedex.withAbility(pokedex.ability(AbilityName("Intimidate"))!!)
        assertTrue("incineroar" in intimidate.ids())
        // a hidden ability too
        assertTrue(pokedex.withAbility(pokedex.ability(AbilityName("Cud Chew"))!!).any { it.id == "tauros-paldea-aqua" })
    }

    @Test
    fun theLearnersOfAMoveIncludeTheMegas() = runTest {
        val pokedex = pokedex()
        val learners = pokedex.learners(pokedex.move(MoveName("Flamethrower"))!!).ids()
        assertTrue("charizard" in learners)
        assertTrue("charizard-mega-y" in learners)
    }

    @Test
    fun aCombinedSearch() = runTest {
        val pokedex = pokedex()
        // the Water Pokémon learning Trick Room, with a base speed below 50
        val filter = PokedexFilter(
            types = setOf(PokeType.WATER),
            learns = setOf(pokedex.move(MoveName("Trick Room"))!!),
            stats = listOf(StatFilter(Stat.SPEED, max = 49)),
        )
        assertEquals(listOf("slowbro", "slowbro-mega", "slowking"), pokedex.search(filter).ids())
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
        val forms = listOf("charizard", "charizard-mega-x", "charizard-mega-y")
        assertEquals(forms, pokedex.formsOf(charizard).ids())
        assertEquals(forms, pokedex.formsOf(megaY).ids())
        assertEquals(megaY.moves, charizard.moves)
        assertNotNull(megaY.megaStone)
    }

    @Test
    fun theFormsAreAllTheFormsOfTheSpecies() = runTest {
        val pokedex = pokedex()
        fun formsOf(name: String) = pokedex.formsOf(pokedex.species(PokemonName(name))!!).ids()
        val raichus = listOf("raichu", "raichu-alola", "raichu-mega-x", "raichu-mega-y")
        assertEquals(raichus.toSet(), formsOf("Raichu").toSet())
        assertEquals(formsOf("Raichu"), formsOf("Raichu-Alola"))
        assertEquals("raichu", formsOf("Raichu-Mega-Y").first())
        assertEquals(6, formsOf("Rotom-Wash").size)
        // the forms only taken in battle
        assertEquals(listOf("morpeko", "morpeko-hangry"), formsOf("Morpeko"))
        assertTrue(pokedex.species(PokemonName("Morpeko-Hangry"))!!.isBattleOnly)
        // a species whose base form isn't legal
        assertEquals(listOf("floette-eternal", "floette-mega"), formsOf("Floette-Mega"))
        assertEquals(listOf("kommo-o"), formsOf("Kommo-o"))
    }

    @Test
    fun theMovepoolIsSortedByName() = runTest {
        val pokedex = pokedex()
        val names = pokedex.movepool(pokedex.species(PokemonName("Incineroar"))!!).map { it.name.en }
        assertEquals(names.sorted(), names)
        assertTrue("Fake Out" in names)
    }

    @Test
    fun aSearchByEnglishOrFrenchName() = runTest {
        val pokedex = pokedex()
        assertEquals("charizard", pokedex.searchByName("Charizard").first().id)
        assertEquals("charizard", pokedex.searchByName("dracaufeu").first().id)
        assertEquals("trick-room", pokedex.searchByName("Distorsion").first().id)
    }

    @Test
    fun aSearchIgnoresAccentsCaseAndPunctuation() = runTest {
        val pokedex = pokedex()
        assertEquals("charizard-mega-y", pokedex.searchByName("mega dracaufeu y").first().id)
        assertEquals("charizard-mega-y", pokedex.searchByName("MÉGA-DRACAUFEU-Y").first().id)
        assertEquals("kommo-o", pokedex.searchByName("kommo o").first().id)
        assertEquals("kommo-o", pokedex.searchByName("ekaiser").first().id)
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
    fun aNameWrittenOtherwiseIsFound() = runTest {
        val pokedex = pokedex()
        // the normalized name first
        assertEquals("king's-shield", pokedex.move(MoveName("king's-shield"))?.id)
        // else the folded one, e.g. with another apostrophe, without punctuation or accents
        assertEquals("king's-shield", pokedex.move(MoveName("King’s Shield"))?.id)
        assertEquals("king's-shield", pokedex.move(MoveName("Kings Shield"))?.id)
        assertEquals("king's-rock", pokedex.item(ItemName("kingsrock"))?.id)
        assertEquals("farfetch’d", pokedex.species(PokemonName("Farfetch'd"))?.id)
        assertEquals("mr.-rime", pokedex.species(PokemonName("Mr Rime"))?.id)
        assertEquals("intimidate", pokedex.ability(AbilityName("INTIMIDATE"))?.id)
        assertEquals(null, pokedex.move(MoveName("Not A Move")))
    }

    @Test
    fun aFormsNameIsShownWithoutDashes() = runTest {
        val pokedex = pokedex()
        fun displayName(name: String) = pokedex.species(PokemonName(name))?.displayName
        assertEquals("Mega Absol Z", displayName("Absol-Mega-Z"))
        assertEquals("Mega Charizard Y", displayName("Charizard-Mega-Y"))
        assertEquals("Tauros Paldea Aqua", displayName("Tauros-Paldea-Aqua"))
        assertEquals("Floette Eternal", displayName("Floette-Eternal"))
        // a base form's dash is part of its name
        assertEquals("Kommo-o", displayName("Kommo-o"))
        assertEquals("Mr. Rime", displayName("Mr. Rime"))
        assertEquals("Incineroar", displayName("Incineroar"))
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
