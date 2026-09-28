package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import com.tambapps.pokemon.champions.data.Move
import com.tambapps.pokemon.champions.engine.BattlePokemon
import com.tambapps.pokemon.champions.engine.MoveDamageResult

sealed interface DamageCalcResult {

    /**
     * A calc that could be run
     */
    data class Success(
        val attacker: BattlePokemon,
        val defender: BattlePokemon,
        val move: Move,
        val damage: MoveDamageResult,
    ) : DamageCalcResult {
        val hits get() = damage.hits.size
        val isImmune get() = damage.hits.all { it.typeEffectiveness == 0.0 }

        val damageRangeText get() = "${damage.minDamage} - ${damage.maxDamage}"
        val damagePercentText get() =
            "${formatPercent(damage.minDamage, defender.maxHp)} - ${formatPercent(damage.maxDamage, defender.maxHp)}%"

        /**
         * The KO chance shown on the result: the source calculator's text, counting the hazards and end-of-turn effects
         * of the defender's side (e.g. "guaranteed 3HKO after Sitrus Berry recovery", see [MoveDamageResult.koChance]),
         * except for an immunity, where the source's "No damage for you" is replaced by a clearer text
         */
        val koChanceText get() = if (isImmune) "doesn't affect ${defender.species.name.value}" else damage.koChance.text

        // fractions of the defender's max HP, for the HP bar
        val currentHpFraction get() = defender.hp.toFloat() / defender.maxHp
        val damageFractionRange get() =
            (damage.minDamage.toFloat() / defender.maxHp)..(damage.maxDamage.toFloat() / defender.maxHp)

        /**
         * The damage part of the calc description, like the source calculator: "min-max (min% - max%)",
         * or "0 (0%)" when both percentages are equal
         */
        val damageText: String get() {
            val minPercent = formatPercent(damage.minDamage, defender.maxHp)
            val maxPercent = formatPercent(damage.maxDamage, defender.maxHp)
            return if (minPercent != maxPercent) "${damage.minDamage}-${damage.maxDamage} ($minPercent - $maxPercent%)"
            else "${damage.minDamage} ($maxPercent%)"
        }

        /**
         * The source calculator's description of the calc, e.g. "+1 32+ Atk Life Orb Tough Claws Mega Charizard X Flare Blitz
         * vs. 32 HP  / 0 Def Incineroar in Sun through Reflect: [damageText] -- guaranteed OHKO".
         * Everything but [damageText] is exactly the source's, KO chance text included even for an immunity, see
         * [MoveDamageResult.description] and [MoveDamageResult.koChance].
         */
        val description: String get() = "${damage.description}: $damageText -- ${damage.koChance.text}"
    }

    data class Error(val message: String) : DamageCalcResult
}

/**
 * [value] out of [total] as a percentage floored to one decimal, like the source calculator (e.g. "93.4")
 */
internal fun formatPercent(value: Int, total: Int): String = formatTenths(value * 1000L / total)

private fun formatTenths(tenths: Long): String {
    val decimal = tenths % 10
    return if (decimal == 0L) "${tenths / 10}" else "${tenths / 10}.$decimal"
}
