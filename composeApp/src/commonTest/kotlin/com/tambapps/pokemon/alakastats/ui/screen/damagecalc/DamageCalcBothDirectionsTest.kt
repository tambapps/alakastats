package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import com.tambapps.pokemon.PokemonName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** The results of both pokemon's moves, for the desktop layout. */
class DamageCalcBothDirectionsTest {

    private fun viewModel() = DamageCalcViewModel().apply {
        attacker.selectSpecies(PokemonName("Garchomp"))
        attacker.ability = "Rough Skin"
        attacker.setMove(0, "Earthquake")
        defender.selectSpecies(PokemonName("Toxapex"))
        defender.ability = "Regenerator"
        defender.setMove(0, "Thunderbolt")
        defender.setMove(1, "Protect")
    }

    @Test
    fun theDefendersMovesTargetTheAttacker() {
        val result = assertIs<DamageCalcResult.Success>(viewModel().resultOf(DamageCalcSide.DEFENDER, 0))
        assertEquals("Toxapex", result.attacker.species.name.value)
        assertEquals("Garchomp", result.defender.species.name.value)
        assertEquals("Thunderbolt", result.move.name.value)
    }

    @Test
    fun selectingADefenderMoveDisplaysItsResult() {
        val viewModel = viewModel()
        viewModel.selectMove(DamageCalcSide.DEFENDER, 0)
        assertEquals(viewModel.resultOf(DamageCalcSide.DEFENDER, 0), viewModel.result)
    }

    @Test
    fun theDefendersOwnSideAppliesToItsMoves() {
        // not Garchomp, immune to the defender's Thunderbolt
        val viewModel = viewModel().apply { attacker.selectSpecies(PokemonName("Corviknight")) }
        val normalDamage = assertIs<DamageCalcResult.Success>(viewModel.resultOf(DamageCalcSide.DEFENDER, 0)).damage.maxDamage
        viewModel.updateField(viewModel.field.copy(defenderSide = viewModel.field.defenderSide.copy(isCharged = true)))
        val chargedDamage = assertIs<DamageCalcResult.Success>(viewModel.resultOf(DamageCalcSide.DEFENDER, 0)).damage.maxDamage
        assertTrue(chargedDamage > normalDamage)
    }

    @Test
    fun everyMoveOfBothPokemonHasAResult() {
        val viewModel = viewModel()
        val results = viewModel.moveResults
        assertEquals(viewModel.attacker.moves.size, results.getValue(DamageCalcSide.ATTACKER).size)
        assertEquals(viewModel.defender.moves.size, results.getValue(DamageCalcSide.DEFENDER).size)
        assertEquals(DamageCalcResult.Error("Protect is a status move"), results.getValue(DamageCalcSide.DEFENDER)[1])
    }

    @Test
    fun swapSelectsTheNewAttackersFirstMove() {
        val viewModel = viewModel().apply { selectMove(DamageCalcSide.DEFENDER, 1) }
        viewModel.swap()
        assertEquals(DamageCalcSide.ATTACKER, viewModel.selectedMoveSide)
        assertEquals(0, viewModel.selectedMoveIndex)
    }
}
