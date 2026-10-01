package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.Stat
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Filling a pokemon from the first pokemon of a pokepaste */
class DamageCalcPokepasteFillTest {

    private fun filledFrom(pokepaste: String) = DamageCalcViewModel().attacker.apply {
        fillFrom(PokepasteParser().parse(pokepaste).pokemons.first())
    }

    @Test
    fun theSetIsFilled() {
        val pokemon = filledFrom(
            """
            Incineroar @ Sitrus Berry
            Ability: Intimidate
            EVs: 32 HP / 2 Atk / 20 Def / 12 SpD
            Adamant Nature
            - Fake Out
            - Flare Blitz
            - Knock Off
            - Parting Shot

            Garchomp @ Choice Scarf
            Ability: Rough Skin
            - Earthquake
            """.trimIndent()
        )
        assertEquals("Incineroar", pokemon.name.value)
        assertEquals("Intimidate", pokemon.ability)
        assertEquals("Sitrus Berry", pokemon.item)
        assertEquals(Nature.ADAMANT, pokemon.nature)
        assertEquals(listOf("Fake Out", "Flare Blitz", "Knock Off", "Parting Shot"), pokemon.moves)
        assertEquals(32, pokemon.getStatPoints(Stat.HP))
        assertEquals(2, pokemon.getStatPoints(Stat.ATTACK))
        assertEquals(20, pokemon.getStatPoints(Stat.DEFENSE))
        assertEquals(0, pokemon.getStatPoints(Stat.SPECIAL_ATTACK))
        assertEquals(12, pokemon.getStatPoints(Stat.SPECIAL_DEFENSE))
    }

    @Test
    fun evsAreConvertedToStatPoints() {
        val pokemon = filledFrom(
            """
            Incineroar
            Ability: Intimidate
            EVs: 252 HP / 4 Atk / 156 Def / 96 SpD
            Careful Nature
            - Fake Out
            """.trimIndent()
        )
        assertEquals(32, pokemon.getStatPoints(Stat.HP))
        assertEquals(1, pokemon.getStatPoints(Stat.ATTACK))
        assertEquals(20, pokemon.getStatPoints(Stat.DEFENSE))
        assertEquals(12, pokemon.getStatPoints(Stat.SPECIAL_DEFENSE))
        assertEquals(0, pokemon.getStatPoints(Stat.SPEED))
        assertEquals(listOf("Fake Out", "", "", ""), pokemon.moves)
    }

    private val incineroarPaste = """
        Incineroar @ Sitrus Berry
        Ability: Intimidate
        EVs: 252 HP / 4 Atk / 156 Def / 96 SpD
        Careful Nature
        - Fake Out
        - Flare Blitz
    """.trimIndent()

    private val incineroarSet get() = PokepasteParser().parse(incineroarPaste).pokemons.first()

    @Test
    fun aFilledPokemonHasTheSet() {
        val pokemon = filledFrom(incineroarPaste).apply {
            // moves and battle settings don't matter
            setMove(0, "Knock Off")
            setBoost(Stat.ATTACK, 1)
            setCurrentHpPercent(50)
        }
        assertTrue(pokemon.hasSetOf(incineroarSet))
    }

    @Test
    fun aChangedAbilityItemNatureOrStatPointsIsNotTheSet() {
        assertFalse(filledFrom(incineroarPaste).apply { ability = "Blaze" }.hasSetOf(incineroarSet))
        assertFalse(filledFrom(incineroarPaste).apply { item = "Assault Vest" }.hasSetOf(incineroarSet))
        assertFalse(filledFrom(incineroarPaste).apply { nature = Nature.ADAMANT }.hasSetOf(incineroarSet))
        assertFalse(filledFrom(incineroarPaste).apply { setStatPoints(Stat.SPEED, 1) }.hasSetOf(incineroarSet))
        assertFalse(filledFrom(incineroarPaste).apply { selectSpecies(PokemonName("Garchomp")) }.hasSetOf(incineroarSet))
    }

    @Test
    fun evsToStatPointsIsTheInverseOfTheSourceDisplay() {
        // the source calculator displays stat points as max(0, 8 * stat points - 4) EVs
        for (statPoints in 0..MAX_STAT_POINTS_PER_STAT) {
            assertEquals(statPoints, evsToStatPoints(maxOf(0, 8 * statPoints - 4)))
        }
        assertEquals(0, evsToStatPoints(3))
        assertEquals(32, evsToStatPoints(255))
    }
}
