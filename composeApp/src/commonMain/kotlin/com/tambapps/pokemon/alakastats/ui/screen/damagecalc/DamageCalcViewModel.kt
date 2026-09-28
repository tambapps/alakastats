package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import com.tambapps.pokemon.Gender
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.champions.data.Ability
import com.tambapps.pokemon.alakastats.ui.util.VoidSignal
import com.tambapps.pokemon.champions.data.ChampionsCalcException
import com.tambapps.pokemon.champions.data.MoveCategory
import com.tambapps.pokemon.champions.engine.Battlefield
import com.tambapps.pokemon.champions.engine.DamageCalculator
import com.tambapps.pokemon.champions.engine.Terrain
import com.tambapps.pokemon.champions.engine.Weather
import com.tambapps.pokemon.champions.engine.terrainSetOnField
import com.tambapps.pokemon.champions.engine.weatherSetOnField

class DamageCalcViewModel : ScreenModel {

    // TODO dummy values, will be fetched later
    var attacker by mutableStateOf(
        DamageCalcPokemonState(
            name = PokemonName("Aegislash"),
            ability = "Stance Change",
            item = "Spell Tag",
            moves = listOf("Poltergeist", "Shadow Sneak", "Iron Head", "Sacred Sword"),
        )
    )
        private set
    var defender by mutableStateOf(
        DamageCalcPokemonState(
            name = PokemonName("incineroar"),
            ability = "Intimidate",
            item = "Sitrus Berry",
            moves = listOf("Fake Out", "Flare Blitz", "Knock Off", "Parting Shot"),
        )
    )
        private set
    // the engine's model is used directly, as every UI state of the field is a valid Battlefield
    var field by mutableStateOf(Battlefield())
        private set
    var selectedMoveIndex by mutableIntStateOf(0)
    val scrollToTopSignal = VoidSignal()
    // abilities setting the weather/terrain on the field, like the source calculator (e.g. Drought -> Sun)
    private val weatherSync = AbilityFieldSync(Weather.NONE)
    private val terrainSync = AbilityFieldSync(Terrain.NONE)
    // the Fairy Aura value last set manually, restored when no pokemon has Fairy Aura anymore (the source's lastManualField)
    private var manualFairyAura = false

    init {
        attacker.onAbilityChange = ::onAbilityChange
        defender.onAbilityChange = ::onAbilityChange
        // like the source calculator when loading the pokemons
        onAbilityChange(attacker, isAbilityChange = true)
        onAbilityChange(defender, isAbilityChange = true)
    }

    /**
     * Change the field, as the user did on the Field page
     */
    fun updateField(newField: Battlefield) {
        if (newField.isFairyAura != field.isFairyAura) {
            manualFairyAura = newField.isFairyAura
        }
        field = newField
    }

    private fun onAbilityChange(pokemon: DamageCalcPokemonState, isAbilityChange: Boolean) {
        val side = if (pokemon === attacker) 0 else 1
        val ability = pokemon.resolvedAbility
        field = field.copy(
            weather = weatherSync.onAbilityChange(field.weather, side, ability.weatherSetOnField(pokemon.isAbilityActive)),
            terrain = terrainSync.onAbilityChange(field.terrain, side, ability.terrainSetOnField(pokemon.isAbilityActive)),
        )
        if (isAbilityChange) {
            // like the source calculator's setIndependentField: Fairy Aura affects the whole field, so a pokemon
            // having it turns it on, and it's back to its manual value once no pokemon has it
            val hasFairyAura = attacker.resolvedAbility == Ability.FAIRY_AURA || defender.resolvedAbility == Ability.FAIRY_AURA
            field = field.copy(isFairyAura = hasFairyAura || manualFairyAura)
        }
    }

    // recomputed whenever an input of the calc changes
    val result: DamageCalcResult by derivedStateOf { computeResult() }

    private fun computeResult(): DamageCalcResult {
        val (attackerGender, defenderGender) = rivalryGenders()
        val attackerPokemon = attacker.toBattlePokemon()?.copy(gender = attackerGender)
            ?: return DamageCalcResult.Error("${attacker.form.pretty} isn't a Champions Pokemon")
        val defenderPokemon = defender.toBattlePokemon()?.copy(gender = defenderGender)
            ?: return DamageCalcResult.Error("${defender.form.pretty} isn't a Champions Pokemon")
        val moveName = attacker.moves.getOrNull(selectedMoveIndex)?.takeIf { it.isNotBlank() }
            ?: return DamageCalcResult.Error("Select a move")
        val move = attacker.championsMove(selectedMoveIndex)
            ?: return DamageCalcResult.Error("$moveName isn't a Champions move")
        if (move.category == MoveCategory.STATUS) {
            return DamageCalcResult.Error("${move.name.value} is a status move")
        }
        return try {
            val damage = DamageCalculator.calculateMove(
                attacker = attackerPokemon,
                defender = defenderPokemon,
                moveUse = attacker.moveUse(
                    index = selectedMoveIndex,
                    // Counter-like moves return the defender's move, with the defender's settings for it
                    counteredMove = defender.moveUse(attacker.counteredMoveIndex(selectedMoveIndex)),
                )!!,
                field = field,
                hits = attacker.hitCount(selectedMoveIndex),
            )
            DamageCalcResult.Success(
                attacker = attackerPokemon,
                defender = defenderPokemon,
                move = move,
                damage = damage,
            )
        } catch (e: ChampionsCalcException) {
            DamageCalcResult.Error(e.message ?: "Couldn't run the calc")
        }
    }

    /**
     * The genders of the attacker and defender. The source calculator's Rivalry setting relates the Rivalry pokemon's
     * gender to its target's, while the engine takes genders: genderless unless a Rivalry setting is on (the attacker's
     * one first, as only the attacker's Rivalry affects its damage)
     */
    private fun rivalryGenders(): Pair<Gender, Gender> {
        val relation = listOf(attacker, defender)
            .firstOrNull { it.resolvedAbility == Ability.RIVALRY && it.rivalry != RivalryRelation.OFF }
            ?.rivalry
        return when (relation) {
            RivalryRelation.SAME -> Gender.MALE to Gender.MALE
            RivalryRelation.OPPOSITE -> Gender.MALE to Gender.FEMALE
            RivalryRelation.OFF, null -> Gender.ASEXUAL to Gender.ASEXUAL
        }
    }

    fun pokemonState(side: DamageCalcSide) = when (side) {
        DamageCalcSide.ATTACKER -> attacker
        DamageCalcSide.DEFENDER -> defender
    }

    fun swap() {
        val previousAttacker = attacker
        attacker = defender
        defender = previousAttacker
        // side conditions follow the pokemon, as does what their ability set on the field
        field = field.copy(attackerSide = field.defenderSide, defenderSide = field.attackerSide)
        weatherSync.swapSides()
        terrainSync.swapSides()
        selectedMoveIndex = 0
    }
}
