package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import com.tambapps.pokemon.AbilityName
import com.tambapps.pokemon.ItemName
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.Stat
import com.tambapps.pokemon.champions.engine.StatBoosts
import com.tambapps.pokemon.champions.engine.Status
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DamageCalcPokemonStateTest {

    private fun state(name: String = "incineroar", ability: String = "Intimidate", item: String = "Sitrus Berry") =
        DamageCalcPokemonState(PokemonName(name), ability, item, moves = emptyList())

    @Test
    fun convertsToBattlePokemon() {
        val state = state().apply {
            nature = Nature.ADAMANT
            status = Status.BURNED
            setStatPoints(Stat.HP, 32)
            setStatPoints(Stat.ATTACK, 20)
            setBoost(Stat.ATTACK, 2)
            setBoost(Stat.SPEED, -1)
        }
        val battlePokemon = state.toBattlePokemon()!!

        assertEquals(AbilityName("Intimidate"), battlePokemon.ability)
        assertEquals(ItemName("Sitrus Berry"), battlePokemon.item)
        assertEquals(Nature.ADAMANT, battlePokemon.nature)
        assertEquals(Status.BURNED, battlePokemon.status)
        assertEquals(PokeStats(hp = 32, speed = 0, attack = 20, specialAttack = 0, defense = 0, specialDefense = 0), battlePokemon.statPoints)
        assertEquals(StatBoosts(attack = 2, speed = -1), battlePokemon.boosts)
        assertNull(battlePokemon.currentHp)
    }

    @Test
    fun finalStatsAreTheEnginesComputedStats() {
        val state = state().apply {
            nature = Nature.ADAMANT
            setStatPoints(Stat.ATTACK, 32)
            // boosts don't change the displayed stat
            setBoost(Stat.ATTACK, 2)
        }
        val stats = state.stats!!
        assertEquals(state.toBattlePokemon()!!.stats, stats)
        val neutral = state().apply { setStatPoints(Stat.ATTACK, 32) }.stats!!
        // Adamant boosts Attack
        assertTrue(stats.attack > neutral.attack)
    }

    @Test
    fun noFinalStatsForSpeciesUnknownToChampions() {
        assertNull(state(name = "not-a-real-pokemon").stats)
    }

    @Test
    fun returnsNullForSpeciesUnknownToChampions() {
        assertNull(state(name = "not-a-real-pokemon").toBattlePokemon())
    }

    @Test
    fun blankItemMeansNoItem() {
        assertNull(state(item = "").toBattlePokemon()!!.item)
    }

    @Test
    fun convertsCurrentHpPercentToHpPoints() {
        val state = state().apply { currentHpPercent = 50 }
        val battlePokemon = state.toBattlePokemon()!!
        assertEquals(battlePokemon.maxHp * 50 / 100, battlePokemon.currentHp)
    }

    @Test
    fun zeroPercentHpKeepsOneHpPoint() {
        assertEquals(1, state().apply { currentHpPercent = 0 }.toBattlePokemon()!!.currentHp)
    }

    @Test
    fun selectingASpeciesUsesItsDefaultAbility() {
        val state = state().apply { selectSpecies(PokemonName("Sylveon")) }
        assertEquals("Pixilate", state.ability)
    }

    @Test
    fun selectingAPokemonSelectsItsFirstForm() {
        val state = state().apply { selectSpecies(PokemonName("Aegislash")) }
        assertEquals(listOf(PokemonName("Aegislash-Shield"), PokemonName("Aegislash-Blade")), state.availableForms)
        assertEquals(PokemonName("Aegislash-Shield"), state.form)
    }

    @Test
    fun aSingleFormPokemonIsItsOwnForm() {
        // the form has the dex's spelling, whatever the picked name's
        val state = state(name = "incineroar")
        assertEquals(listOf(PokemonName("Incineroar")), state.availableForms)
        assertEquals(PokemonName("Incineroar"), state.form)
    }

    @Test
    fun selectingAMegaFormUsesItsAbilityAndHoldsItsStone() {
        val state = state().apply {
            selectSpecies(PokemonName("Charizard"))
            selectForm(PokemonName("Mega Charizard X"))
        }
        assertEquals("Tough Claws", state.ability)
        assertEquals("Charizardite X", state.item)
    }

    @Test
    fun selectingANonMegaFormKeepsTheItem() {
        val state = state().apply {
            selectSpecies(PokemonName("Aegislash"))
            selectForm(PokemonName("Aegislash-Blade"))
        }
        assertEquals("Sitrus Berry", state.item)
    }

    @Test
    fun convertsTheSelectedForm() {
        val state = state().apply {
            selectSpecies(PokemonName("Charizard"))
            selectForm(PokemonName("Mega Charizard Y"))
        }
        assertEquals(PokemonName("Mega Charizard Y"), state.toBattlePokemon()!!.species.name)
    }

    @Test
    fun selectingAnUnknownSpeciesKeepsTheAbility() {
        val state = state().apply { selectSpecies(PokemonName("not-a-real-pokemon")) }
        assertEquals("Intimidate", state.ability)
    }
}
