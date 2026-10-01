package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import com.tambapps.pokemon.PokemonName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The mega switch replacing the form selection of a pokemon holding its mega stone */
class DamageCalcMegaSwitchTest {

    private fun charizard(item: String) = DamageCalcViewModel().attacker.apply {
        selectSpecies(PokemonName("Charizard"))
        this.item = item
    }

    @Test
    fun theHeldStoneGivesTheMegaForm() {
        assertEquals("Mega Charizard X", charizard("Charizardite X").megaFormOfItem?.value)
        assertEquals("Mega Charizard Y", charizard("Charizardite Y").megaFormOfItem?.value)
    }

    @Test
    fun noMegaStoneNoSwitch() {
        assertNull(charizard("Leftovers").megaFormOfItem)
        assertNull(charizard("").megaFormOfItem)
    }

    @Test
    fun otherFormsThanMegasHaveNoSwitch() {
        val aegislash = DamageCalcViewModel().attacker.apply { selectSpecies(PokemonName("Aegislash")) }
        assertNull(aegislash.megaFormOfItem)
    }

    @Test
    fun megaEvolvingAndBack() {
        val pokemon = charizard("Charizardite Y").apply { ability = "Blaze" }
        assertFalse(pokemon.isMegaEvolved)

        pokemon.setMegaEvolved(true)
        assertTrue(pokemon.isMegaEvolved)
        assertEquals("Mega Charizard Y", pokemon.form.value)
        assertEquals("Drought", pokemon.ability)
        assertEquals("Charizardite Y", pokemon.item)

        pokemon.setMegaEvolved(false)
        assertFalse(pokemon.isMegaEvolved)
        assertEquals("Charizard", pokemon.form.value)
        // the ability it had before
        assertEquals("Blaze", pokemon.ability)
    }
}
