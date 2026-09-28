package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.champions.engine.Weather
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The move types shown with the moves (type icons) */
class DamageCalcMoveTypeTest {

    private fun viewModel() = DamageCalcViewModel().apply {
        attacker.selectSpecies(PokemonName("Pelipper"))
        attacker.ability = "Keen Eye"
        attacker.setMove(0, "Weather Ball")
        attacker.setMove(1, "Protect")
        attacker.setMove(2, "")
        defender.selectSpecies(PokemonName("Incineroar"))
    }

    @Test
    fun aMoveHasItsOwnTypeWithoutChange() {
        assertEquals(PokeType.NORMAL, viewModel().moveTypeOf(DamageCalcSide.ATTACKER, 0))
    }

    @Test
    fun aMoveHasTheTypeItHasInTheCalc() {
        val viewModel = viewModel()
        viewModel.updateField(viewModel.field.copy(weather = Weather.RAIN))
        assertEquals(PokeType.WATER, viewModel.moveTypeOf(DamageCalcSide.ATTACKER, 0))
    }

    @Test
    fun aStatusMoveHasItsOwnType() {
        assertEquals(PokeType.NORMAL, viewModel().moveTypeOf(DamageCalcSide.ATTACKER, 1))
    }

    @Test
    fun anEmptyMoveHasNoType() {
        assertNull(viewModel().moveTypeOf(DamageCalcSide.ATTACKER, 2))
    }
}
