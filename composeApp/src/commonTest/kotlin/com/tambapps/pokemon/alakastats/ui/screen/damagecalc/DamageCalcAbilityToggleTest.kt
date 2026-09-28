package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.champions.engine.Terrain
import com.tambapps.pokemon.champions.engine.Weather
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DamageCalcAbilityToggleTest {

    private fun viewModel() = DamageCalcViewModel().apply {
        attacker.selectSpecies(PokemonName("Garchomp"))
        attacker.ability = "Rough Skin"
        attacker.setMove(0, "Earthquake")
        selectedMoveIndex = 0
        defender.selectSpecies(PokemonName("Toxapex"))
        defender.ability = "Regenerator"
    }

    private val DamageCalcViewModel.success get() = assertIs<DamageCalcResult.Success>(result)

    @Test
    fun toggleDefaultsFollowTheSourceCalculator() {
        val state = viewModel().attacker
        state.ability = "Intimidate"
        assertTrue(state.hasAbilityToggle)
        assertTrue(state.isAbilityActive)
        state.ability = "Flash Fire"
        assertTrue(state.hasAbilityToggle)
        assertFalse(state.isAbilityActive)
        state.ability = "Rough Skin"
        assertFalse(state.hasAbilityToggle)
    }

    @Test
    fun changingTheAbilityResetsTheToggle() {
        val state = viewModel().attacker
        state.ability = "Protean"
        state.isAbilityActive = false
        state.ability = "Libero"
        assertTrue(state.isAbilityActive)
    }

    @Test
    fun toggleIsPassedToTheEngine() {
        val state = viewModel().attacker
        state.ability = "Flash Fire"
        assertFalse(state.toBattlePokemon()!!.abilityIsActive)
        state.isAbilityActive = true
        assertTrue(state.toBattlePokemon()!!.abilityIsActive)
    }

    @Test
    fun activeIntimidateLowersTheAttackersDamage() {
        val viewModel = viewModel()
        val damageWithoutIntimidate = viewModel.success.damage.maxDamage
        viewModel.defender.ability = "Intimidate"
        assertTrue(viewModel.defender.isAbilityActive)
        val damageWithIntimidate = viewModel.success.damage.maxDamage
        assertTrue(damageWithIntimidate < damageWithoutIntimidate)
        viewModel.defender.isAbilityActive = false
        assertEquals(damageWithoutIntimidate, viewModel.success.damage.maxDamage)
    }

    @Test
    fun weatherAbilitySetsTheWeatherAndRestoresTheManualOne() {
        val viewModel = viewModel()
        viewModel.updateField(viewModel.field.copy(weather = Weather.RAIN))
        viewModel.attacker.ability = "Drought"
        assertEquals(Weather.SUN, viewModel.field.weather)
        viewModel.attacker.ability = "Rough Skin"
        assertEquals(Weather.RAIN, viewModel.field.weather)
    }

    @Test
    fun theOtherSidesWeatherTakesOverWhenAnAbilityIsRemoved() {
        val viewModel = viewModel()
        viewModel.attacker.ability = "Drought"
        viewModel.defender.ability = "Drizzle"
        assertEquals(Weather.RAIN, viewModel.field.weather)
        viewModel.defender.ability = "Regenerator"
        assertEquals(Weather.SUN, viewModel.field.weather)
    }

    @Test
    fun sandSpitOnlySetsSandWhenActive() {
        val viewModel = viewModel()
        viewModel.attacker.ability = "Sand Spit"
        assertEquals(Weather.NONE, viewModel.field.weather)
        viewModel.attacker.isAbilityActive = true
        assertEquals(Weather.SAND, viewModel.field.weather)
        viewModel.attacker.isAbilityActive = false
        assertEquals(Weather.NONE, viewModel.field.weather)
    }

    @Test
    fun surgeAbilitySetsTheTerrain() {
        val viewModel = viewModel()
        viewModel.defender.ability = "Psychic Surge"
        assertEquals(Terrain.PSYCHIC, viewModel.field.terrain)
        viewModel.defender.ability = "Regenerator"
        assertEquals(Terrain.NONE, viewModel.field.terrain)
    }

    @Test
    fun swapKeepsWhatEachAbilitySetWithItsPokemon() {
        val viewModel = viewModel()
        viewModel.attacker.ability = "Drought"
        viewModel.swap()
        assertEquals(Weather.SUN, viewModel.field.weather)
        // the Drought pokemon is now the defender
        viewModel.defender.ability = "Rough Skin"
        assertEquals(Weather.NONE, viewModel.field.weather)
    }

    @Test
    fun fairyAuraPokemonTurnsFairyAuraOnForTheWholeField() {
        val viewModel = viewModel()
        assertFalse(viewModel.field.isFairyAura)
        // on either side, as it affects every pokemon's Fairy moves
        viewModel.defender.ability = "Fairy Aura"
        assertTrue(viewModel.field.isFairyAura)
        viewModel.defender.ability = "Regenerator"
        assertFalse(viewModel.field.isFairyAura)
        viewModel.attacker.ability = "Fairy Aura"
        assertTrue(viewModel.field.isFairyAura)
    }

    @Test
    fun fairyAuraPokemonBoostsFairyMovesWithoutTouchingTheField() {
        val viewModel = viewModel().apply {
            attacker.selectSpecies(PokemonName("Floette-Eternal"))
            attacker.selectForm(PokemonName("Mega Floette"))
            attacker.setMove(0, "Moonblast")
        }
        assertEquals("Fairy Aura", viewModel.attacker.ability)
        assertTrue(viewModel.field.isFairyAura)
        val boostedDamage = assertIs<DamageCalcResult.Success>(viewModel.result).damage.maxDamage
        viewModel.updateField(viewModel.field.copy(isFairyAura = false))
        assertTrue(assertIs<DamageCalcResult.Success>(viewModel.result).damage.maxDamage < boostedDamage)
    }

    @Test
    fun manualFairyAuraIsRestoredWhenNoPokemonHasItAnymore() {
        val viewModel = viewModel()
        viewModel.updateField(viewModel.field.copy(isFairyAura = true))
        viewModel.attacker.ability = "Fairy Aura"
        viewModel.attacker.ability = "Rough Skin"
        assertTrue(viewModel.field.isFairyAura)
    }

    @Test
    fun manuallyTurningFairyAuraOffLastsUntilTheNextAbilityChange() {
        val viewModel = viewModel().apply {
            attacker.ability = "Fairy Aura"
            defender.ability = "Intimidate"
        }
        viewModel.updateField(viewModel.field.copy(isFairyAura = false))
        // an ability toggle isn't an ability change, like in the source calculator
        viewModel.defender.isAbilityActive = false
        assertFalse(viewModel.field.isFairyAura)
        // the next ability change turns it back on, as a pokemon still has Fairy Aura
        viewModel.defender.ability = "Regenerator"
        assertTrue(viewModel.field.isFairyAura)
    }

    @Test
    fun weatherPokemonLoadedFromTheStartSetsTheWeather() {
        val viewModel = DamageCalcViewModel()
        // the default defender has Intimidate, which doesn't set any weather
        assertEquals(Weather.NONE, viewModel.field.weather)
        assertTrue(viewModel.defender.isAbilityActive)
    }
}
