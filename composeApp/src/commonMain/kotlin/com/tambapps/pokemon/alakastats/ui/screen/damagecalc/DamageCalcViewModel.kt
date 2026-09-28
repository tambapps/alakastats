package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.ui.util.VoidSignal
import com.tambapps.pokemon.champions.engine.Battlefield

class DamageCalcViewModel : ScreenModel {

    // TODO dummy values, will be fetched later
    var attacker by mutableStateOf(
        DamageCalcPokemonState(
            name = PokemonName("aegislash-shield"),
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
    var selectedMoveIndex by mutableIntStateOf(0)
    val scrollToTopSignal = VoidSignal()

    // TODO dummy result, will be computed later
    val damageRangeText get() = "104 - 126"
    val damagePercentText get() = "62.2 - 75.4%"
    val koChanceText get() = "guaranteed 2HKO"
    val damagePercentRange get() = 0.622f..0.754f
    val calcDescription get() = "+1 32+ Atk Spell Tag Aegislash-Shield Poltergeist vs. 32 HP  / 0 Def Aegislash-Shield: " +
            "156-186 (93.4 - 111.3%) -- 62.5% chance to OHKO"

    fun pokemonState(side: DamageCalcSide) = when (side) {
        DamageCalcSide.ATTACKER -> attacker
        DamageCalcSide.DEFENDER -> defender
    }

    fun swap() {
        val previousAttacker = attacker
        attacker = defender
        defender = previousAttacker
        // side conditions follow the pokemon
        field = field.copy(attackerSide = field.defenderSide, defenderSide = field.attackerSide)
        selectedMoveIndex = 0
    }
}
