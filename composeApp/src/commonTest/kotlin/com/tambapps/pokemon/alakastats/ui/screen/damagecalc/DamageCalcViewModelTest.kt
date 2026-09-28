package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.Stat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DamageCalcViewModelTest {

    private fun viewModel(attackerName: String = "Garchomp", attackerMove: String = "Earthquake") = DamageCalcViewModel().apply {
        attacker.selectSpecies(PokemonName(attackerName))
        attacker.setMove(0, attackerMove)
        selectedMoveIndex = 0
        defender.selectSpecies(PokemonName("Toxapex"))
    }

    @Test
    fun calculatesTheSelectedMove() {
        val result = assertIs<DamageCalcResult.Success>(viewModel().result)
        assertEquals("Earthquake", result.move.name.value)
        assertEquals(1, result.hits)
        assertTrue(result.damage.maxDamage > 0)
    }

    @Test
    fun emptyMoveIsAnError() {
        val viewModel = viewModel().apply { attacker.setMove(0, "") }
        assertEquals(DamageCalcResult.Error("Select a move"), viewModel.result)
    }

    @Test
    fun statusMoveIsAnError() {
        val viewModel = viewModel(attackerMove = "Protect")
        assertEquals(DamageCalcResult.Error("Protect is a status move"), viewModel.result)
    }

    @Test
    fun immunityDoesNotAffectTheDefender() {
        val viewModel = viewModel().apply { defender.selectSpecies(PokemonName("Corviknight")) }
        val result = assertIs<DamageCalcResult.Success>(viewModel.result)
        assertTrue(result.isImmune)
        assertEquals("doesn't affect Corviknight", result.koChanceText)
    }

    @Test
    fun multiHitMoveUsesTheDefaultHitCount() {
        val viewModel = viewModel(attackerMove = "Bullet Seed")
        assertEquals(2..5, viewModel.attacker.selectableHitCounts(0))
        assertEquals(3, assertIs<DamageCalcResult.Success>(viewModel.result).hits)
    }

    @Test
    fun skillLinkChangesTheDefaultHitCount() {
        val viewModel = viewModel(attackerMove = "Bullet Seed").apply { attacker.ability = "Skill Link" }
        assertEquals(5, assertIs<DamageCalcResult.Success>(viewModel.result).hits)
    }

    @Test
    fun selectedHitCountIsUsed() {
        val viewModel = viewModel(attackerMove = "Bullet Seed").apply { attacker.selectHitCount(0, 2) }
        assertEquals(2, assertIs<DamageCalcResult.Success>(viewModel.result).hits)
    }

    @Test
    fun changingTheMoveResetsTheSelectedHitCount() {
        val viewModel = viewModel(attackerMove = "Bullet Seed").apply {
            attacker.selectHitCount(0, 2)
            attacker.setMove(0, "Rock Blast")
        }
        assertEquals(3, viewModel.attacker.hitCount(0))
    }

    @Test
    fun singleHitMoveHasNoSelectableHitCount() {
        assertEquals(null, viewModel().attacker.selectableHitCounts(0))
    }

    @Test
    fun describesTheCalc() {
        val viewModel = viewModel().apply {
            attacker.nature = Nature.ADAMANT
            attacker.item = "Life Orb"
            attacker.ability = "Rough Skin"
            attacker.setStatPoints(Stat.ATTACK, 32)
            attacker.setBoost(Stat.ATTACK, 1)
            defender.setStatPoints(Stat.HP, 32)
        }
        val result = assertIs<DamageCalcResult.Success>(viewModel.result)
        assertEquals(
            "+1 32+ Atk Life Orb Rough Skin Garchomp Earthquake vs. 32 HP / 0 Def Toxapex: " +
                "${result.damage.minDamage}-${result.damage.maxDamage} (${result.damagePercentText}) -- ${result.koChanceText}",
            result.description
        )
    }

    @Test
    fun describesTheHitsOfAMultiHitMove() {
        val result = assertIs<DamageCalcResult.Success>(viewModel(attackerMove = "Bullet Seed").result)
        assertTrue("Bullet Seed (3 hits) vs." in result.description)
    }

    @Test
    fun formatsPercentagesLikeTheSourceCalculator() {
        assertEquals("33.3", formatPercent(1, 3))
        assertEquals("100", formatPercent(3, 3))
        assertEquals("62.5", formatPercent(0.625))
        assertEquals("100", formatPercent(1.0))
    }
}
