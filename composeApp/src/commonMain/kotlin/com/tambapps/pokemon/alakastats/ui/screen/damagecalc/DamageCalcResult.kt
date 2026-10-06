package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.champions.data.Move
import com.tambapps.pokemon.champions.engine.BattlePokemon
import com.tambapps.pokemon.champions.engine.KoChance
import com.tambapps.pokemon.champions.engine.MoveDamageResult
import com.tambapps.pokemon.champions.engine.description.CalcFacts

sealed interface DamageCalcResult {

    /**
     * This result if the calc could be run, null otherwise
     */
    fun asSuccess(): Success? = this as? Success

    /**
     * A calc that could be run
     */
    data class Success(
        val attacker: BattlePokemon,
        val defender: BattlePokemon,
        val move: Move,
        /** The type the move has in this calc, which can differ from [Move.type] (e.g. Weather Ball in Rain, Pixilate) */
        val moveType: PokeType,
        val damage: MoveDamageResult,
    ) : DamageCalcResult {
        val hits get() = damage.hits.size
        val isImmune get() = damage.hits.all { it.typeEffectiveness == 0.0 }
        // an immunity, or a fixed damage of 0
        val dealsNoDamage get() = damage.koChance.kind == KoChance.Kind.NO_DAMAGE

        val damageRangeText get() = "${damage.minDamage} - ${damage.maxDamage}"
        val damagePercentText get() =
            "${formatPercent(damage.minDamage, defender.maxHp)} - ${formatPercent(damage.maxDamage, defender.maxHp)}%"

        /**
         * The damage shown on the result: the percentages, or what makes it deal none (clearer than "0 - 0%")
         */
        val shownDamageText get() = when {
            isImmune -> "Immune"
            dealsNoDamage -> "No damage"
            else -> damagePercentText
        }

        /**
         * The KO chance shown on the result: the source calculator's text, counting the hazards and end-of-turn effects
         * of the defender's side (e.g. "guaranteed 3HKO after Sitrus Berry recovery", see [MoveDamageResult.koChance]),
         * except where it's replaced by a clearer text: an immunity (the source's "No damage for you") and a move not
         * KOing in the max uses looked at (the source's "possibly the worst move ever")
         */
        val koChanceText get() = when {
            isImmune -> "doesn't affect ${defender.species.name.value}"
            damage.koChance.kind == KoChance.Kind.NO_KO_IN_MAX_USES -> "${KoChance.MAX_KO_USES + 1}HKO or more"
            else -> damage.koChance.text
        }

        /**
         * The field conditions that changed the damage, e.g. "Sun" only for a move it boosts or weakens, "Light Screen"
         * only for a special move: the ones the calc's description mentions (see [CalcFacts]), and the spread move
         * reduction in doubles. Hazards and end-of-turn effects aren't, being in [koChanceText]
         */
        val fieldEffects: List<String> get() {
            val facts = damage.facts
            return listOfNotNull(
                facts.weather?.displayName,
                facts.terrain?.let { "${it.displayName} Terrain" },
                facts.screen?.displayName,
                "Spread (×0.75)".takeIf { facts.isSpread },
                "Helping Hand".takeIf { facts.isHelpingHand },
                "Power Spot".takeIf { facts.isPowerSpot },
                "Battery".takeIf { facts.isBattery },
                "Steely Spirit".takeIf { facts.isAllySteelySpirit },
                "Charge".takeIf { facts.isCharged },
                "Fairy Aura".takeIf { facts.isFairyAuraBoosted },
                "Gravity".takeIf { facts.isGravity },
                "Friend Guard".takeIf { facts.isFriendGuard },
                "Protect".takeIf { facts.isQuarteredByProtect },
            )
        }

        // empty when nothing of the field changed the damage
        val fieldEffectsText get() = fieldEffects.joinToString(" · ")

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
