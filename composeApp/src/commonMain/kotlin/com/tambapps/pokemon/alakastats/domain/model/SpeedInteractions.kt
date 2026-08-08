package com.tambapps.pokemon.alakastats.domain.model

import com.tambapps.pokemon.ItemName
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.PokeStats

enum class SpeedComparisonResult {
    FASTER, SLOWER, GUARANTEED_FASTER, GUARANTEED_SLOWER, SPEED_TIE
}

const val SPEED_INTERACTIONS_QUIZ_LEVEL = 50

val NEUTRAL_INVESTMENT_EVS: PokeStats = PokeStats.default(0)
val NEUTRAL_INVESTMENT_IVS: PokeStats = PokeStats.default(31)
val SPEED_NEUTRAL_NATURE: Nature = Nature.QUIRKY
val SPEED_BOOST_NATURE: Nature = Nature.JOLLY
val SPEED_MALUS_NATURE: Nature = Nature.BRAVE

fun isHoldingChoiceScarf(item: ItemName?) = item?.normalized?.value == "choice-scarf"

fun maxSpeedInvestmentEvs(legacySystem: Boolean): Int = if (legacySystem) 252 else 32

/**
 * B's legal Speed range boundaries (min/neutral/max investment), at [SPEED_INTERACTIONS_QUIZ_LEVEL].
 */
data class SpeedRange(val min: Int, val neutral: Int, val max: Int)

fun computeSpeedRange(baseStats: PokeStats, legacySystem: Boolean): SpeedRange {
    val min = PokeStats.compute(
        baseStats = baseStats,
        evs = PokeStats.default(0),
        ivs = PokeStats.default(0),
        nature = SPEED_MALUS_NATURE,
        level = SPEED_INTERACTIONS_QUIZ_LEVEL,
        legacySystem = legacySystem
    ).speed
    val neutral = PokeStats.compute(
        baseStats = baseStats,
        evs = NEUTRAL_INVESTMENT_EVS,
        ivs = NEUTRAL_INVESTMENT_IVS,
        nature = SPEED_NEUTRAL_NATURE,
        level = SPEED_INTERACTIONS_QUIZ_LEVEL,
        legacySystem = legacySystem
    ).speed
    val max = PokeStats.compute(
        baseStats = baseStats,
        evs = PokeStats.default(maxSpeedInvestmentEvs(legacySystem)),
        ivs = PokeStats.default(31),
        nature = SPEED_BOOST_NATURE,
        level = SPEED_INTERACTIONS_QUIZ_LEVEL,
        legacySystem = legacySystem
    ).speed
    return SpeedRange(min, neutral, max)
}

/**
 * Answer key for team mode: compares A's known exact speed [sA] against B's legal [range].
 * Open intervals; landing exactly on any of the three references is a tie.
 */
fun speedComparisonResultFor(sA: Int, range: SpeedRange): SpeedComparisonResult = when {
    sA == range.max || sA == range.neutral || sA == range.min -> SpeedComparisonResult.SPEED_TIE
    sA > range.max -> SpeedComparisonResult.GUARANTEED_FASTER
    sA > range.neutral -> SpeedComparisonResult.FASTER
    sA > range.min -> SpeedComparisonResult.SLOWER
    else -> SpeedComparisonResult.GUARANTEED_SLOWER
}
