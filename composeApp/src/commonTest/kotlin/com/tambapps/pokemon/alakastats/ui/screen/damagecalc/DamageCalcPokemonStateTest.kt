package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import com.tambapps.pokemon.AbilityName
import com.tambapps.pokemon.ItemName
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.Stat
import com.tambapps.pokemon.champions.engine.StatBoosts
import com.tambapps.pokemon.champions.engine.Status
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DamageCalcPokemonStateTest {

    private fun state(name: String = "incineroar", ability: String = "Intimidate", item: String = "Sitrus Berry") =
        DamageCalcPokemonState(PokemonName(name), ability, item, moves = emptyList())

    @Test
    fun convertsToBattlePokemon() {
        val state = state().apply {
            nature = Nature.ADAMANT
            status = Status.BURNED
            setStatPoints(Stat.HP, 32)
            setStatPoints(Stat.ATTACK, 20)
            setBoost(Stat.ATTACK, 2)
            setBoost(Stat.SPEED, -1)
        }
        val battlePokemon = state.toBattlePokemon()!!

        assertEquals(AbilityName("Intimidate"), battlePokemon.ability)
        assertEquals(ItemName("Sitrus Berry"), battlePokemon.item)
        assertEquals(Nature.ADAMANT, battlePokemon.nature)
        assertEquals(Status.BURNED, battlePokemon.status)
        assertEquals(PokeStats(hp = 32, speed = 0, attack = 20, specialAttack = 0, defense = 0, specialDefense = 0), battlePokemon.statPoints)
        assertEquals(StatBoosts(attack = 2, speed = -1), battlePokemon.boosts)
        // full HP by default
        assertEquals(battlePokemon.maxHp, battlePokemon.currentHp)
    }

    @Test
    fun finalStatsAreTheEnginesComputedStats() {
        val state = state().apply {
            nature = Nature.ADAMANT
            setStatPoints(Stat.ATTACK, 32)
            // boosts don't change the displayed stat
            setBoost(Stat.ATTACK, 2)
        }
        val stats = state.stats!!
        assertEquals(state.toBattlePokemon()!!.stats, stats)
        val neutral = state().apply { setStatPoints(Stat.ATTACK, 32) }.stats!!
        // Adamant boosts Attack
        assertTrue(stats.attack > neutral.attack)
    }

    @Test
    fun noFinalStatsForSpeciesUnknownToChampions() {
        assertNull(state(name = "not-a-real-pokemon").stats)
    }

    @Test
    fun returnsNullForSpeciesUnknownToChampions() {
        assertNull(state(name = "not-a-real-pokemon").toBattlePokemon())
    }

    @Test
    fun blankItemMeansNoItem() {
        assertNull(state(item = "").toBattlePokemon()!!.item)
    }

    @Test
    fun fullHpByDefault() {
        val state = state()
        assertEquals(100, state.currentHpPercent)
        assertEquals(state.maxHp, state.currentHp)
        assertEquals(state.maxHp, state.toBattlePokemon()!!.currentHp)
    }

    @Test
    fun percentageIsConvertedToHpPointsRoundedUpLikeTheSource() {
        // with an odd max HP, 50% isn't a whole number of HP points
        val state = state()
        val oddMaxHpStatPoints = (0..MAX_STAT_POINTS_PER_STAT).first { statPoints ->
            state.setStatPoints(Stat.HP, statPoints)
            state.maxHp!! % 2 == 1
        }
        state.setStatPoints(Stat.HP, oddMaxHpStatPoints)
        state.setCurrentHpPercent(50)
        val maxHp = state.maxHp!!
        assertEquals(maxHp / 2 + 1, state.currentHp)
        assertEquals(state.currentHp, state.toBattlePokemon()!!.currentHp)
    }

    @Test
    fun exactHpPointsAreKeptAndThePercentageRoundedDown() {
        val state = state()
        val maxHp = state.maxHp!!
        state.setCurrentHp(101)
        assertEquals(101, state.currentHp)
        assertEquals(101, state.toBattlePokemon()!!.currentHp)
        assertEquals(100 * 101 / maxHp, state.currentHpPercent)
    }

    @Test
    fun hpPointsAreClampedBetweenZeroAndTheMaxHp() {
        val state = state()
        state.setCurrentHp(-5)
        assertEquals(0, state.currentHp)
        state.setCurrentHp(10_000)
        assertEquals(state.maxHp, state.currentHp)
    }

    @Test
    fun changingTheMaxHpRecomputesTheHpFromThePercentageLikeTheSource() {
        val state = state().apply { setCurrentHp(101) }
        val percent = state.currentHpPercent
        state.setStatPoints(Stat.HP, 32)
        val maxHp = state.maxHp!!
        assertEquals((percent * maxHp + 99) / 100, state.currentHp)
    }

    @Test
    fun changingTheFormKeepsTheMissingHpLikeTheSource() {
        val state = state(name = "Charizard").apply { selectSpecies(PokemonName("Charizard")) }
        val missingHp = 20
        state.setCurrentHp(state.maxHp!! - missingHp)
        state.selectForm(PokemonName("Mega Charizard X"))
        assertEquals(state.maxHp!! - missingHp, state.currentHp)
    }

    @Test
    fun selectingAnotherPokemonResetsToFullHp() {
        val state = state().apply { setCurrentHpPercent(30) }
        state.selectSpecies(PokemonName("Sylveon"))
        assertEquals(100, state.currentHpPercent)
        assertEquals(state.maxHp, state.currentHp)
    }

    @Test
    fun selectingASpeciesUsesItsDefaultAbility() {
        val state = state().apply { selectSpecies(PokemonName("Sylveon")) }
        assertEquals("Pixilate", state.ability)
    }

    @Test
    fun selectingAPokemonSelectsItsFirstForm() {
        val state = state().apply { selectSpecies(PokemonName("Aegislash")) }
        assertEquals(listOf(PokemonName("Aegislash-Shield"), PokemonName("Aegislash-Blade")), state.availableForms)
        assertEquals(PokemonName("Aegislash-Shield"), state.form)
    }

    @Test
    fun aSingleFormPokemonIsItsOwnForm() {
        // the form has the dex's spelling, whatever the picked name's
        val state = state(name = "incineroar")
        assertEquals(listOf(PokemonName("Incineroar")), state.availableForms)
        assertEquals(PokemonName("Incineroar"), state.form)
    }

    @Test
    fun selectingAMegaFormUsesItsAbilityAndHoldsItsStone() {
        val state = state().apply {
            selectSpecies(PokemonName("Charizard"))
            selectForm(PokemonName("Mega Charizard X"))
        }
        assertEquals("Tough Claws", state.ability)
        assertEquals("Charizardite X", state.item)
    }

    @Test
    fun selectingANonMegaFormKeepsTheItem() {
        val state = state().apply {
            selectSpecies(PokemonName("Aegislash"))
            selectForm(PokemonName("Aegislash-Blade"))
        }
        assertEquals("Sitrus Berry", state.item)
    }

    @Test
    fun convertsTheSelectedForm() {
        val state = state().apply {
            selectSpecies(PokemonName("Charizard"))
            selectForm(PokemonName("Mega Charizard Y"))
        }
        assertEquals(PokemonName("Mega Charizard Y"), state.toBattlePokemon()!!.species.name)
    }

    @Test
    fun selectingAnUnknownSpeciesKeepsTheAbility() {
        val state = state().apply { selectSpecies(PokemonName("not-a-real-pokemon")) }
        assertEquals("Intimidate", state.ability)
    }
}
