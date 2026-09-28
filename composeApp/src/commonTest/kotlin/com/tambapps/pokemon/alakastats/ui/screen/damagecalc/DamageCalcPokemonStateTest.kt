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
    fun selectingAnUnknownSpeciesKeepsTheAbility() {
        val state = state().apply { selectSpecies(PokemonName("not-a-real-pokemon")) }
        assertEquals("Intimidate", state.ability)
    }
}
