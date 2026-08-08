package com.tambapps.pokemon.alakastats.domain.model

import com.tambapps.pokemon.ItemName
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.PokeStats

enum class SpeedComparisonResult {
    // Home mode
    FASTER, SLOWER, SPEED_TIE,
    // Team mode
    GUARANTEED_FASTER, GUARANTEED_SLOWER, DEPENDS_ON_INVESTMENT
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
 * At most one of these applies to a given team-mode question (never stacked).
 */
enum class SpeedModifier {
    NONE, B_CHOICE_SCARF, B_PARALYSIS, A_PARALYSIS, A_TAILWIND
}

fun applyChoiceScarf(speed: Int): Int = (speed * 1.5f).toInt()
fun applyTailwind(speed: Int): Int = speed * 2
fun applyParalysis(speed: Int): Int = speed / 2

/**
 * Team mode reveals B's Speed nature but hides its stat-point investment (mirrors Open Team Sheets).
 */
enum class RevealedNature(val nature: Nature) {
    BOOSTING(SPEED_BOOST_NATURE),
    NEUTRAL(SPEED_NEUTRAL_NATURE),
    REDUCING(SPEED_MALUS_NATURE)
}

/**
 * B's legal Speed bounds at its [RevealedNature], from 0 to max stat-point investment.
 */
data class OpponentSpeedRange(val min: Int, val max: Int)

fun computeOpponentSpeedRange(baseStats: PokeStats, revealedNature: RevealedNature, legacySystem: Boolean): OpponentSpeedRange {
    val min = PokeStats.compute(
        baseStats = baseStats,
        evs = PokeStats.default(0),
        ivs = PokeStats.default(31),
        nature = revealedNature.nature,
        level = SPEED_INTERACTIONS_QUIZ_LEVEL,
        legacySystem = legacySystem
    ).speed
    val max = PokeStats.compute(
        baseStats = baseStats,
        evs = PokeStats.default(maxSpeedInvestmentEvs(legacySystem)),
        ivs = PokeStats.default(31),
        nature = revealedNature.nature,
        level = SPEED_INTERACTIONS_QUIZ_LEVEL,
        legacySystem = legacySystem
    ).speed
    return OpponentSpeedRange(min, max)
}

/**
 * Smallest Speed stat-point investment (at [revealedNature]) for [baseStats] to reach or pass [mine],
 * after applying [transform] (the same modifier, if any, used to derive the displayed range) to each
 * scanned value. Null if [mine] is never reached within the legal investment range.
 */
fun minInvestmentToReach(
    baseStats: PokeStats,
    revealedNature: RevealedNature,
    legacySystem: Boolean,
    mine: Int,
    transform: (Int) -> Int = { it }
): Int? {
    val maxEv = maxSpeedInvestmentEvs(legacySystem)
    for (ev in 0..maxEv) {
        val speed = transform(
            PokeStats.compute(
                baseStats = baseStats,
                evs = PokeStats.default(ev),
                ivs = PokeStats.default(31),
                nature = revealedNature.nature,
                level = SPEED_INTERACTIONS_QUIZ_LEVEL,
                legacySystem = legacySystem
            ).speed
        )
        if (speed >= mine) return ev
    }
    return null
}

/**
 * Team-mode answer key: compares A's known exact Speed [mine] against B's [range] at its revealed
 * nature. Boundary ties fall inside DEPENDS_ON_INVESTMENT (surfaced in feedback, not a separate answer).
 */
fun speedComparisonResultFor(mine: Int, range: OpponentSpeedRange): SpeedComparisonResult = when {
    mine > range.max -> SpeedComparisonResult.GUARANTEED_FASTER
    mine < range.min -> SpeedComparisonResult.GUARANTEED_SLOWER
    else -> SpeedComparisonResult.DEPENDS_ON_INVESTMENT
}
