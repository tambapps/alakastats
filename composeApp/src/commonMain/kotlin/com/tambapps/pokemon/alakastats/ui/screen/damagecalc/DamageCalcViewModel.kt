package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.ui.util.VoidSignal
import com.tambapps.pokemon.champions.data.ChampionsCalcException
import com.tambapps.pokemon.champions.data.MoveCategory
import com.tambapps.pokemon.champions.engine.Battlefield
import com.tambapps.pokemon.champions.engine.DamageCalculator
import com.tambapps.pokemon.champions.engine.KoChanceCalculator
import com.tambapps.pokemon.champions.engine.MoveUse

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
    var selectedMoveIndex by mutableIntStateOf(0)
    val scrollToTopSignal = VoidSignal()

    // recomputed whenever an input of the calc changes
    val result: DamageCalcResult by derivedStateOf { computeResult() }

    private fun computeResult(): DamageCalcResult {
        val attackerPokemon = attacker.toBattlePokemon()
            ?: return DamageCalcResult.Error("${attacker.form.pretty} isn't a Champions Pokemon")
        val defenderPokemon = defender.toBattlePokemon()
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
                moveUse = MoveUse(move, isCritical = attacker.isCritical(selectedMoveIndex)),
                field = field,
                hits = attacker.hitCount(selectedMoveIndex),
            )
            DamageCalcResult.Success(
                attacker = attackerPokemon,
                defender = defenderPokemon,
                move = move,
                damage = damage,
                koChance = if (damage.maxDamage > 0) {
                    KoChanceCalculator.minimumUsesToKo(damage, targetHp = defenderPokemon.hp, maxUses = MAX_USES_TO_KO)
                } else null,
            )
        } catch (e: ChampionsCalcException) {
            DamageCalcResult.Error(e.message ?: "Couldn't run the calc")
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
        // side conditions follow the pokemon
        field = field.copy(attackerSide = field.defenderSide, defenderSide = field.attackerSide)
        selectedMoveIndex = 0
    }
}
