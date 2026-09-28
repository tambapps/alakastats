package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.Stat
import com.tambapps.pokemon.champions.engine.SideConditions
import com.tambapps.pokemon.champions.engine.Status
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
        assertEquals(0, result.damage.maxDamage)
        // the source calculator's text (tools/oracle.js in the pokemon repo)
        assertEquals("No damage for you", result.koChanceText)
    }

    // the KO chance texts below are the source calculator's for the same calcs (tools/oracle.js in the pokemon repo).
    // The defender still holds the Sitrus Berry of the view model's default defender

    @Test
    fun koChanceCountsTheBerry() {
        val result = assertIs<DamageCalcResult.Success>(viewModel().result)
        assertEquals("87.5% chance to 2HKO after Sitrus Berry recovery", result.koChanceText)
    }

    @Test
    fun koChanceCountsTheDefenderSideHazardsAndEndOfTurnEffects() {
        val viewModel = viewModel().apply {
            field = field.copy(defenderSide = SideConditions(hasStealthRock = true, isLeechSeeded = true))
        }
        val result = assertIs<DamageCalcResult.Success>(viewModel.result)
        assertEquals("guaranteed 2HKO after Stealth Rock, Leech Seed damage, and Sitrus Berry recovery", result.koChanceText)
    }

    @Test
    fun koChanceIgnoresTheAttackerSideHazards() {
        val viewModel = viewModel().apply {
            field = field.copy(attackerSide = SideConditions(hasStealthRock = true, isLeechSeeded = true))
        }
        val result = assertIs<DamageCalcResult.Success>(viewModel.result)
        assertEquals("87.5% chance to 2HKO after Sitrus Berry recovery", result.koChanceText)
    }

    @Test
    fun koChanceCountsSpikesLayers() {
        val viewModel = viewModel().apply {
            field = field.copy(defenderSide = field.defenderSide.withNextSpikesLayer().withNextSpikesLayer())
        }
        assertEquals(2, viewModel.field.defenderSide.spikesLayers)
        val result = assertIs<DamageCalcResult.Success>(viewModel.result)
        assertEquals("guaranteed 2HKO after 2 layers of Spikes and Sitrus Berry recovery", result.koChanceText)
    }

    @Test
    fun koChanceCountsTheToxicCounter() {
        val viewModel = viewModel().apply {
            defender.status = Status.BADLY_POISONED
            defender.toxicCounter = 5
        }
        val result = assertIs<DamageCalcResult.Success>(viewModel.result)
        assertEquals(5, result.defender.toxicCounter)
        assertEquals("guaranteed 2HKO after toxic damage and Sitrus Berry recovery", result.koChanceText)
    }

    @Test
    fun spikesChipCyclesThroughTheLayers() {
        var conditions = SideConditions.NONE
        val texts = (0..4).map {
            spikesChipText(conditions.spikesLayers).also { conditions = conditions.withNextSpikesLayer() }
        }
        assertEquals(listOf("Spikes", "Spikes ×1", "Spikes ×2", "Spikes ×3", "Spikes"), texts)
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
        // everything but the damage numbers is the source calculator's output for the same calc (tools/oracle.js in the pokemon repo)
        assertEquals(
            "+1 32+ Atk Life Orb Garchomp Earthquake vs. 32 HP  / 0 Def Toxapex: 190-226 (121 - 143.9%) -- guaranteed OHKO",
            result.description
        )
    }

    @Test
    fun describesAnImmunityWithASinglePercentage() {
        val viewModel = viewModel().apply { defender.selectSpecies(PokemonName("Corviknight")) }
        val result = assertIs<DamageCalcResult.Success>(viewModel.result)
        assertEquals("Garchomp Earthquake vs. Corviknight: 0 (0%) -- No damage for you", result.description)
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
    }
}
