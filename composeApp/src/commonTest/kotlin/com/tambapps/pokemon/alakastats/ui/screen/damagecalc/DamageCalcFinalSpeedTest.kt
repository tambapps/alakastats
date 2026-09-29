package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.Stat
import com.tambapps.pokemon.champions.engine.Battlefield
import com.tambapps.pokemon.champions.engine.SideConditions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** The speed shown in the stats, the one the calc uses */
class DamageCalcFinalSpeedTest {

    private fun incineroar() = DamageCalcViewModel().attacker.apply {
        selectSpecies(PokemonName("Incineroar"))
        item = ""
    }

    private val speedStat get() = assertNotNull(incineroar().stats?.get(Stat.SPEED))

    @Test
    fun withoutModifierItsTheSpeedStat() {
        assertEquals(speedStat, incineroar().finalSpeed(Battlefield(), DamageCalcSide.ATTACKER))
    }

    @Test
    fun boostsChangeIt() {
        val pokemon = incineroar().apply { setBoost(Stat.SPEED, 2) }
        assertEquals(speedStat * 2, pokemon.finalSpeed(Battlefield(), DamageCalcSide.ATTACKER))
    }

    @Test
    fun choiceScarfChangesIt() {
        val pokemon = incineroar().apply { item = "Choice Scarf" }
        assertEquals(speedStat * 3 / 2, pokemon.finalSpeed(Battlefield(), DamageCalcSide.ATTACKER))
    }

    @Test
    fun onlyTheTailwindOfItsSideChangesIt() {
        val field = Battlefield(defenderSide = SideConditions(hasTailwind = true))
        assertEquals(speedStat, incineroar().finalSpeed(field, DamageCalcSide.ATTACKER))
        assertEquals(speedStat * 2, incineroar().finalSpeed(field, DamageCalcSide.DEFENDER))
    }
}
