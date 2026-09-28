package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import com.tambapps.pokemon.champions.data.Move
import com.tambapps.pokemon.champions.engine.BattlePokemon
import com.tambapps.pokemon.champions.engine.KoChanceResult
import com.tambapps.pokemon.champions.engine.MoveDamageResult

sealed interface DamageCalcResult {

    /**
     * A calc that could be run
     *
     * @param koChance null if the move can't KO within the max number of uses
     */
    data class Success(
        val attacker: BattlePokemon,
        val defender: BattlePokemon,
        val move: Move,
        val damage: MoveDamageResult,
        val koChance: KoChanceResult?,
    ) : DamageCalcResult {
        val hits get() = damage.hits.size
        val isImmune get() = damage.hits.all { it.typeEffectiveness == 0.0 }

        val damageRangeText get() = "${damage.minDamage} - ${damage.maxDamage}"
        val damagePercentText get() =
            "${formatPercent(damage.minDamage, defender.maxHp)} - ${formatPercent(damage.maxDamage, defender.maxHp)}%"

        val koChanceText get() = when {
            isImmune -> "doesn't affect ${defender.species.name.value}"
            koChance == null -> "can't KO in $MAX_USES_TO_KO uses"
            koChance.isGuaranteed -> "guaranteed ${hkoText(koChance.hits)}"
            else -> "${formatPercent(koChance.chance)}% chance to ${hkoText(koChance.hits)}"
        }

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
         * vs. 32 HP  / 0 Def Incineroar in Sun through Reflect: [damageText] -- [koChanceText]".
         * Everything before " -- " is exactly the source's, see [MoveDamageResult.description].
         */
        val description: String get() = "${damage.description}: $damageText -- $koChanceText"
    }

    data class Error(val message: String) : DamageCalcResult
}

// the max number of uses of a move considered for the KO chance
const val MAX_USES_TO_KO = 4

private fun hkoText(uses: Int) = if (uses == 1) "OHKO" else "${uses}HKO"

/**
 * [value] out of [total] as a percentage floored to one decimal, like the source calculator (e.g. "93.4")
 */
internal fun formatPercent(value: Int, total: Int): String = formatTenths(value * 1000L / total)

/**
 * A probability as a percentage rounded to one decimal (e.g. 0.625 -> "62.5")
 */
internal fun formatPercent(probability: Double): String = formatTenths(kotlin.math.round(probability * 1000).toLong())

private fun formatTenths(tenths: Long): String {
    val decimal = tenths % 10
    return if (decimal == 0L) "${tenths / 10}" else "${tenths / 10}.$decimal"
}
