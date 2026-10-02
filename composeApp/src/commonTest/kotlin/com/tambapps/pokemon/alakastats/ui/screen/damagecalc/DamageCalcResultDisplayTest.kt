package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.Stat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** The texts shown on a result, clearer than some of the source calculator's */
class DamageCalcResultDisplayTest {

    private fun result(attacker: String, ability: String, move: String, defender: String, attackStatPoints: Int = 0) =
        DamageCalcViewModel().run {
            this.attacker.selectSpecies(PokemonName(attacker))
            this.attacker.ability = ability
            this.attacker.item = ""
            this.attacker.setStatPoints(Stat.ATTACK, attackStatPoints)
            this.attacker.setMove(0, move)
            this.defender.selectSpecies(PokemonName(defender))
            this.defender.item = ""
            assertIs<DamageCalcResult.Success>(resultOf(DamageCalcSide.ATTACKER, 0))
        }

    @Test
    fun anImmunityShowsImmune() {
        val result = result("Garchomp", "Rough Skin", "Earthquake", "Corviknight")
        assertTrue(result.dealsNoDamage)
        assertEquals("Immune", result.shownDamageText)
        assertEquals("doesn't affect Corviknight", result.koChanceText)
    }

    @Test
    fun aMoveNotKoingInNineUses() {
        val result = result("Toxapex", "Regenerator", "Fake Out", "Toxapex")
        assertFalse(result.dealsNoDamage)
        assertEquals(result.damagePercentText, result.shownDamageText)
        assertEquals("10HKO or more", result.koChanceText)
        // the copied description keeps the source's text
        assertTrue(result.description.endsWith("possibly the worst move ever"))
    }

    @Test
    fun aKoKeepsTheSourceText() {
        val result = result("Garchomp", "Rough Skin", "Earthquake", "Incineroar", attackStatPoints = 32)
        assertEquals(result.damagePercentText, result.shownDamageText)
        assertEquals(result.damage.koChance.text, result.koChanceText)
    }
}
