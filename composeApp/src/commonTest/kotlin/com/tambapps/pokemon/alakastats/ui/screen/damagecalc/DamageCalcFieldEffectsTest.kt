package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.champions.engine.BattleFormat
import com.tambapps.pokemon.champions.engine.Battlefield
import com.tambapps.pokemon.champions.engine.Weather
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Only the field conditions that changed the damage are shown */
class DamageCalcFieldEffectsTest {

    private fun fieldEffects(move: String, field: (Battlefield) -> Battlefield) = DamageCalcViewModel().run {
        attacker.selectSpecies(PokemonName("Garchomp"))
        attacker.ability = "Rough Skin"
        attacker.item = ""
        attacker.setMove(0, move)
        defender.selectSpecies(PokemonName("Incineroar"))
        defender.ability = "Blaze"
        defender.item = ""
        updateField(field(this.field))
        assertIs<DamageCalcResult.Success>(resultOf(DamageCalcSide.ATTACKER, 0))
    }

    @Test
    fun nothingRelevant() {
        val result = fieldEffects("Dragon Claw") { it.copy(format = BattleFormat.SINGLES) }
        assertEquals(emptyList(), result.fieldEffects)
        assertEquals("", result.fieldEffectsText)
    }

    @Test
    fun lightScreenOnlyForASpecialMove() {
        val lightScreen: (Battlefield) -> Battlefield = { it.copy(defenderSide = it.defenderSide.copy(hasLightScreen = true)) }
        assertEquals(emptyList(), fieldEffects("Dragon Claw", lightScreen).fieldEffects)
        assertEquals(listOf("Light Screen"), fieldEffects("Draco Meteor", lightScreen).fieldEffects)
    }

    @Test
    fun sunOnlyForAMoveItChanges() {
        val sun: (Battlefield) -> Battlefield = { it.copy(weather = Weather.SUN) }
        assertEquals(emptyList(), fieldEffects("Dragon Claw", sun).fieldEffects)
        assertEquals(listOf("Sun"), fieldEffects("Fire Fang", sun).fieldEffects)
    }

    @Test
    fun spreadOnlyInDoubles() {
        assertEquals(listOf("Spread (×0.75)"), fieldEffects("Earthquake") { it.copy(format = BattleFormat.DOUBLES) }.fieldEffects)
        assertEquals(emptyList(), fieldEffects("Earthquake") { it.copy(format = BattleFormat.SINGLES) }.fieldEffects)
    }
}
