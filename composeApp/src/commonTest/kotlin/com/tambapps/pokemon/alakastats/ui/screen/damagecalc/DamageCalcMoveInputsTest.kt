package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import com.tambapps.pokemon.PokemonName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DamageCalcMoveInputsTest {

    private fun viewModel(attackerMove: String) = DamageCalcViewModel().apply {
        attacker.selectSpecies(PokemonName("Garchomp"))
        attacker.ability = "Rough Skin"
        attacker.setMove(0, attackerMove)
        selectedMoveIndex = 0
        defender.selectSpecies(PokemonName("Toxapex"))
        defender.ability = "Regenerator"
    }

    private val DamageCalcViewModel.maxDamage get() = assertIs<DamageCalcResult.Success>(result).damage.maxDamage

    @Test
    fun doubledPowerIncreasesTheDamage() {
        val viewModel = viewModel("Payback")
        val normalDamage = viewModel.maxDamage
        viewModel.attacker.setPowerDoubled(0, true)
        assertTrue(viewModel.attacker.isPowerDoubled(0))
        assertTrue(viewModel.maxDamage > normalDamage)
    }

    @Test
    fun doubledPowerDoesNothingForAMoveThatCantDouble() {
        val viewModel = viewModel("Earthquake")
        val normalDamage = viewModel.maxDamage
        viewModel.attacker.setPowerDoubled(0, true)
        assertFalse(viewModel.attacker.isPowerDoubled(0))
        assertEquals(normalDamage, viewModel.maxDamage)
    }

    @Test
    fun stackedEffectIncreasesTheDamage() {
        val viewModel = viewModel("Last Respects")
        val normalDamage = viewModel.maxDamage
        viewModel.attacker.setStackCount(0, 2)
        assertEquals(2, viewModel.attacker.stackCount(0))
        assertTrue(viewModel.maxDamage > normalDamage)
    }

    @Test
    fun changingTheMoveResetsItsSettings() {
        val viewModel = viewModel("Last Respects").apply {
            attacker.setStackCount(0, 3)
            attacker.setCritical(0, true)
            attacker.setMove(0, "Rage Fist")
        }
        assertEquals(0, viewModel.attacker.stackCount(0))
        assertFalse(viewModel.attacker.isCritical(0))
    }

    @Test
    fun faintedAlliesBoostSupremeOverlord() {
        val viewModel = viewModel("Earthquake").apply { attacker.ability = "Supreme Overlord" }
        val normalDamage = viewModel.maxDamage
        viewModel.attacker.faintedAllyCount = 3
        assertTrue(viewModel.maxDamage > normalDamage)
        // reset when the ability changes, like the source calculator
        viewModel.attacker.ability = "Rough Skin"
        assertEquals(0, viewModel.attacker.faintedAllyCount)
    }

    @Test
    fun rivalryBoostsSameGenderAndWeakensOppositeGenders() {
        val viewModel = viewModel("Earthquake").apply { attacker.ability = "Rivalry" }
        val offDamage = viewModel.maxDamage
        viewModel.attacker.rivalry = RivalryRelation.SAME
        assertTrue(viewModel.maxDamage > offDamage)
        viewModel.attacker.rivalry = RivalryRelation.OPPOSITE
        assertTrue(viewModel.maxDamage < offDamage)
        viewModel.attacker.ability = "Rough Skin"
        assertEquals(RivalryRelation.OFF, viewModel.attacker.rivalry)
    }

    @Test
    fun counterReturnsTheSelectedDefenderMove() {
        val viewModel = viewModel("Counter").apply {
            defender.setMove(0, "Protect")
            defender.setMove(1, "Liquidation")
        }
        // the first defender move by default: a status move, nothing to return
        assertEquals(0, viewModel.attacker.counteredMoveIndex(0))
        assertEquals(0, viewModel.maxDamage)
        viewModel.attacker.setCounteredMoveIndex(0, 1)
        assertTrue(viewModel.maxDamage > 0)
    }
}
