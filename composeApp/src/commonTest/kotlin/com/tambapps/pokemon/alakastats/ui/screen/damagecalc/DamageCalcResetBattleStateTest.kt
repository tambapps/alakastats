package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.Stat
import com.tambapps.pokemon.champions.engine.Status
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DamageCalcResetBattleStateTest {

    private fun incineroar() = DamageCalcViewModel().attacker.apply {
        selectSpecies(PokemonName("Incineroar"))
        ability = "Intimidate"
        item = "Sitrus Berry"
        nature = Nature.ADAMANT
        setStatPoints(Stat.HP, 32)
        setMove(0, "Flare Blitz")
        setMove(1, "Knock Off")
    }

    @Test
    fun aFreshPokemonHasNoBattleState() {
        assertFalse(incineroar().hasBattleState)
    }

    @Test
    fun theBattleStateIsResetAndTheSetKept() {
        val pokemon = incineroar().apply {
            setBoost(Stat.ATTACK, 2)
            status = Status.BADLY_POISONED
            toxicCounter = 3
            setCurrentHpPercent(40)
            setCritical(0, true)
            setPowerDoubled(1, true)
            isAbilityActive = false
        }
        assertTrue(pokemon.hasBattleState)

        pokemon.resetBattleState()

        assertFalse(pokemon.hasBattleState)
        assertEquals(0, pokemon.getBoost(Stat.ATTACK))
        assertEquals(Status.HEALTHY, pokemon.status)
        assertEquals(1, pokemon.toxicCounter)
        assertEquals(100, pokemon.currentHpPercent)
        assertFalse(pokemon.isCritical(0))
        assertFalse(pokemon.isPowerDoubled(1))
        // Intimidate is active by default
        assertTrue(pokemon.isAbilityActive)
        // the set
        assertEquals("Incineroar", pokemon.name.value)
        assertEquals("Intimidate", pokemon.ability)
        assertEquals("Sitrus Berry", pokemon.item)
        assertEquals(Nature.ADAMANT, pokemon.nature)
        assertEquals(32, pokemon.getStatPoints(Stat.HP))
        assertEquals(listOf("Flare Blitz", "Knock Off"), pokemon.moves.take(2))
    }

    @Test
    fun anExactHpBelowTheMaxIsABattleState() {
        val pokemon = incineroar().apply { setCurrentHp(maxHp!! - 1) }
        assertTrue(pokemon.hasBattleState)
        pokemon.resetBattleState()
        assertEquals(pokemon.maxHp, pokemon.currentHp)
    }
}
