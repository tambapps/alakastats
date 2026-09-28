package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.Stat
import com.tambapps.pokemon.champions.data.Move
import com.tambapps.pokemon.champions.engine.BattlePokemon
import com.tambapps.pokemon.champions.engine.BoostableStat
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
         * A Showdown-like description of the calc, e.g. "+1 32+ Atk Spell Tag Stance Change Aegislash-Shield Poltergeist
         * vs. 32 HP / 0 Def Aegislash-Shield: 156 - 186 (93.4 - 111.3%) -- 62.5% chance to OHKO".
         * The attacker's item and ability are always mentioned, even when they don't affect the calc.
         */
        val description: String get() = buildString {
            val attackStat = damage.hits.first().attackStat
            val defenseStat = damage.hits.first().defenseStat
            if (attackStat != null) {
                val statSource = if (attackStat.isDefenderStat) defender else attacker
                appendBoost(statSource.boosts[attackStat.stat])
                append(statText(statSource, attackStat.stat)).append(' ')
            }
            attacker.item?.let { append(it.pretty).append(' ') }
            if (attacker.ability.value.isNotBlank()) append(attacker.ability.pretty).append(' ')
            append(attacker.species.name.value).append(' ')
            append(move.name.value)
            if (hits > 1) append(" ($hits hits)")
            append(" vs. ")
            if (defenseStat != null) {
                appendBoost(defender.boosts[defenseStat])
                append("${defender.statPoints.hp} HP / ").append(statText(defender, defenseStat)).append(' ')
            }
            append(defender.species.name.value)
            append(": ${damage.minDamage}-${damage.maxDamage} (${damagePercentText})")
            append(" -- ").append(koChanceText)
        }
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

private fun StringBuilder.appendBoost(boost: Int) {
    if (boost != 0) append(if (boost > 0) "+$boost " else "$boost ")
}

// e.g. "32+ Atk", with the nature's effect on the stat
private fun statText(pokemon: BattlePokemon, stat: BoostableStat): String {
    val statPoints = pokemon.statPoints[stat.toStat()]
    return "$statPoints${natureSign(pokemon.nature, stat.toStat())} ${stat.showdownName}"
}

private fun natureSign(nature: Nature, stat: Stat) = when (stat) {
    nature.bonusStat -> "+"
    nature.malusStat -> "-"
    else -> ""
}

private val BoostableStat.showdownName get() = when (this) {
    BoostableStat.ATTACK -> "Atk"
    BoostableStat.DEFENSE -> "Def"
    BoostableStat.SPECIAL_ATTACK -> "SpA"
    BoostableStat.SPECIAL_DEFENSE -> "SpD"
    BoostableStat.SPEED -> "Spe"
}
