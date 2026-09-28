package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import com.tambapps.pokemon.PokemonName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DamageCalcCritTest {

    private fun viewModel(attackerMove: String = "Earthquake") = DamageCalcViewModel().apply {
        attacker.selectSpecies(PokemonName("Garchomp"))
        attacker.setMove(0, attackerMove)
        selectedMoveIndex = 0
        defender.selectSpecies(PokemonName("Toxapex"))
    }

    private val DamageCalcViewModel.success get() = assertIs<DamageCalcResult.Success>(result)

    @Test
    fun movesAreNotCriticalByDefault() {
        val viewModel = viewModel()
        assertFalse(viewModel.attacker.isCritical(0))
        assertFalse(viewModel.success.damage.hits.single().isCritical)
    }

    @Test
    fun critToggleCalculatesACriticalHit() {
        val viewModel = viewModel()
        val normalMaxDamage = viewModel.success.damage.maxDamage
        viewModel.attacker.setCritical(0, true)
        assertTrue(viewModel.success.damage.hits.single().isCritical)
        assertTrue(viewModel.success.damage.maxDamage > normalMaxDamage)
    }

    @Test
    fun critIsPerMove() {
        val viewModel = viewModel().apply {
            attacker.setMove(1, "Dragon Claw")
            attacker.setCritical(0, true)
        }
        assertTrue(viewModel.attacker.isCritical(0))
        assertFalse(viewModel.attacker.isCritical(1))
    }

    @Test
    fun alwaysCritMovesAreCriticalWithoutTheToggle() {
        val viewModel = viewModel(attackerMove = "Flower Trick")
        assertTrue(viewModel.attacker.alwaysCrits(0))
        assertTrue(viewModel.attacker.isCritical(0))
        assertTrue(viewModel.success.damage.hits.single().isCritical)
    }

    @Test
    fun multiHitCritAppliesToEveryHit() {
        val viewModel = viewModel(attackerMove = "Bullet Seed").apply { attacker.setCritical(0, true) }
        assertEquals(3, viewModel.success.hits)
        assertTrue(viewModel.success.damage.hits.all { it.isCritical })
    }
}
