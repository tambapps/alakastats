package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.champions.engine.BattleFormat
import com.tambapps.pokemon.champions.engine.SideConditions
import com.tambapps.pokemon.champions.engine.Terrain
import com.tambapps.pokemon.champions.engine.Weather
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DamageCalcResetFieldTest {

    private fun viewModel() = DamageCalcViewModel().apply {
        attacker.selectSpecies(PokemonName("Garchomp"))
        attacker.ability = "Rough Skin"
        defender.selectSpecies(PokemonName("Incineroar"))
        defender.ability = "Intimidate"
    }

    @Test
    fun aFreshFieldHasNothingToReset() {
        assertFalse(viewModel().hasFieldState)
    }

    @Test
    fun theConditionsAreResetAndTheFormatKept() {
        val viewModel = viewModel()
        viewModel.updateField(
            viewModel.field.copy(
                format = BattleFormat.SINGLES,
                weather = Weather.RAIN,
                terrain = Terrain.GRASSY,
                isGravity = true,
                isFairyAura = true,
                attackerSide = SideConditions(hasTailwind = true, hasHelpingHand = true),
                defenderSide = SideConditions(hasReflect = true),
            )
        )
        assertTrue(viewModel.hasFieldState)

        viewModel.resetField()

        assertFalse(viewModel.hasFieldState)
        assertEquals(BattleFormat.SINGLES, viewModel.field.format)
        assertEquals(Weather.NONE, viewModel.field.weather)
        assertEquals(Terrain.NONE, viewModel.field.terrain)
        assertFalse(viewModel.field.isGravity)
        assertFalse(viewModel.field.isFairyAura)
        assertEquals(SideConditions(), viewModel.field.attackerSide)
        assertEquals(SideConditions(), viewModel.field.defenderSide)
    }

    @Test
    fun whatTheAbilitiesSetIsSetAgain() {
        val viewModel = viewModel().apply {
            attacker.selectSpecies(PokemonName("Torkoal"))
            attacker.ability = "Drought"
            defender.selectSpecies(PokemonName("Floette"))
            defender.ability = "Fairy Aura"
        }
        viewModel.updateField(viewModel.field.copy(weather = Weather.RAIN, isGravity = true))

        viewModel.resetField()

        assertEquals(Weather.SUN, viewModel.field.weather)
        assertTrue(viewModel.field.isFairyAura)
        assertFalse(viewModel.field.isGravity)
        assertFalse(viewModel.hasFieldState)
    }
}
