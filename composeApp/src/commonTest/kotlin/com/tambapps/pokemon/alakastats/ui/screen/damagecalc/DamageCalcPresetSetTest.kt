package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import com.tambapps.pokemon.AbilityName
import com.tambapps.pokemon.ItemName
import com.tambapps.pokemon.MoveName
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.Stat
import com.tambapps.pokemon.alakastats.domain.model.PresetSet
import com.tambapps.pokemon.alakastats.domain.model.PresetSets
import kotlin.test.Test
import kotlin.test.assertEquals

class DamageCalcPresetSetTest {

    private val fastMegaY = PresetSet(
        name = "Fast Mega Y",
        species = PokemonName("Charizard"),
        nature = Nature.TIMID,
        ability = AbilityName("Solar Power"),
        item = ItemName("Charizardite Y"),
        statPoints = PokeStats(hp = 2, attack = 0, defense = 0, specialAttack = 32, specialDefense = 0, speed = 32),
        moves = listOf(MoveName("Heat Wave"), MoveName("Solar Beam")),
    )
    private val bulkyMegaX = fastMegaY.copy(
        name = "Bulky Mega X",
        nature = Nature.ADAMANT,
        item = ItemName("Charizardite X"),
        statPoints = PokeStats(hp = 32, attack = 32, defense = 0, specialAttack = 0, specialDefense = 0, speed = 2),
        moves = listOf(MoveName("Flare Blitz"), MoveName("Dragon Claw")),
    )
    private val charizardSets = PresetSets(listOf(fastMegaY, bulkyMegaX))

    private fun pokemon() = DamageCalcViewModel().attacker.apply { presetSetsOf = { charizardSets.of(it) } }

    @Test
    fun pickingASpeciesAppliesItsFirstSet() {
        val pokemon = pokemon().apply { pickSpecies(PokemonName("Charizard")) }
        assertEquals(SetChoice.Preset(fastMegaY), pokemon.setChoice)
        assertEquals("Charizardite Y", pokemon.item)
        assertEquals(Nature.TIMID, pokemon.nature)
        assertEquals(32, pokemon.getStatPoints(Stat.SPECIAL_ATTACK))
        assertEquals(listOf("Heat Wave", "Solar Beam", "", ""), pokemon.moves)
        // not mega evolved: the stone gives a mega switch
        assertEquals("Charizard", pokemon.form.value)
    }

    @Test
    fun pickingASpeciesWithoutSetsKeepsTheDefaultAbility() {
        val pokemon = pokemon().apply { pickSpecies(PokemonName("Garchomp")) }
        assertEquals(emptyList(), pokemon.setChoices.filterIsInstance<SetChoice.Preset>())
        assertEquals("Rough Skin", pokemon.ability)
    }

    @Test
    fun choosingAnotherSet() {
        val pokemon = pokemon().apply {
            pickSpecies(PokemonName("Charizard"))
            chooseSet(SetChoice.Preset(bulkyMegaX))
        }
        assertEquals(SetChoice.Preset(bulkyMegaX), pokemon.setChoice)
        assertEquals("Charizardite X", pokemon.item)
    }

    @Test
    fun theBlankSet() {
        val pokemon = pokemon().apply {
            pickSpecies(PokemonName("Charizard"))
            chooseSet(SetChoice.Blank)
        }
        assertEquals(SetChoice.Blank, pokemon.setChoice)
        assertEquals("", pokemon.item)
        assertEquals(Nature.SERIOUS, pokemon.nature)
        assertEquals(0, pokemon.totalStatPoints)
        assertEquals(listOf("", "", "", ""), pokemon.moves)
    }

    @Test
    fun changingTheSetMakesItCustom() {
        val pokemon = pokemon().apply {
            pickSpecies(PokemonName("Charizard"))
            setStatPoints(Stat.HP, 10)
        }
        assertEquals(SetChoice.Custom, pokemon.setChoice)
    }

    @Test
    fun movesAndBattleSettingsDontChangeTheSet() {
        val pokemon = pokemon().apply {
            pickSpecies(PokemonName("Charizard"))
            setMove(0, "Air Slash")
            setBoost(Stat.SPECIAL_ATTACK, 1)
        }
        assertEquals(SetChoice.Preset(fastMegaY), pokemon.setChoice)
    }

    @Test
    fun megaEvolvingKeepsTheSet() {
        val pokemon = pokemon().apply {
            pickSpecies(PokemonName("Charizard"))
            setMegaEvolved(true)
        }
        assertEquals("Mega Charizard Y", pokemon.form.value)
        assertEquals(SetChoice.Preset(fastMegaY), pokemon.setChoice)
    }
}
