package com.tambapps.pokemon.alakastats.ui.model

import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.alakastats.domain.model.DamageClass
import com.tambapps.pokemon.alakastats.domain.model.MoveClassification
import com.tambapps.pokemon.alakastats.domain.model.PokedexMove
import com.tambapps.pokemon.alakastats.domain.model.foldName

val MoveClassification.label: String
    get() = when (this) {
        MoveClassification.PUNCHING -> "Punching"
        MoveClassification.SOUND -> "Sound-Based"
        MoveClassification.DANCE -> "Dance"
        MoveClassification.SLICING -> "Slicing"
        MoveClassification.WIND -> "Wind"
        MoveClassification.POWDER -> "Powder"
        MoveClassification.BALL_AND_BOMB -> "Ball & Bomb"
        MoveClassification.PULSE -> "Pulse"
        MoveClassification.BITING -> "Biting"
        MoveClassification.EXPLOSIVE -> "Explosive"
        MoveClassification.MENTAL -> "Mental"
        MoveClassification.HEALING -> "Healing"
    }

val DamageClass.label: String get() = name.lowercase().replaceFirstChar { it.uppercase() }

/**
 * Who a move targets, Showdown's targets grouped by what they mean in a double battle
 *
 * @property showdownTargets the targets of Showdown it groups
 */
enum class MoveTargetGroup(val label: String, val showdownTargets: Set<String>) {
    SINGLE("One Pokémon", setOf("normal", "any", "adjacentFoe")),
    ALL_FOES("Both foes", setOf("allAdjacentFoes")),
    ALL_ADJACENT("All others", setOf("allAdjacent")),
    SELF("Self", setOf("self")),
    ALLY("Ally", setOf("adjacentAlly", "adjacentAllyOrSelf")),
    USER_SIDE("User's side", setOf("allySide", "allies", "allyTeam")),
    FOE_SIDE("Foes' side", setOf("foeSide")),
    FIELD("Whole field", setOf("all")),
    VARIES("Random or varies", setOf("randomNormal", "scripted"));

    companion object {
        fun of(move: PokedexMove): MoveTargetGroup? = entries.find { move.target in it.showdownTargets }
    }
}

enum class MovePowerRange(val label: String, val range: IntRange) {
    NONE("No power", 0..0),
    LOW("1–60", 1..60),
    MEDIUM("61–90", 61..90),
    HIGH("91–120", 91..120),
    VERY_HIGH("121+", 121..Int.MAX_VALUE),
}

/** @property matches whether a move's accuracy (null for one that can't miss) is in this range */
enum class MoveAccuracyRange(val label: String, val matches: (Int?) -> Boolean) {
    NEVER_MISSES("Never misses", { it == null }),
    PERFECT("100%", { it == 100 }),
    HIGH("90–99%", { it != null && it in 90..99 }),
    LOW("Below 90%", { it != null && it < 90 }),
}

/**
 * The criteria a move must all meet, null for any
 */
data class PokedexMoveFilters(
    val category: DamageClass? = null,
    val type: PokeType? = null,
    val classification: MoveClassification? = null,
    val target: MoveTargetGroup? = null,
    val priority: Int? = null,
    val pp: Int? = null,
    val power: MovePowerRange? = null,
    val accuracy: MoveAccuracyRange? = null,
) {
    val activeCount get() = listOf(category, type, classification, target, priority, pp, power, accuracy).count { it != null }

    fun matches(move: PokedexMove) =
        (category == null || move.damageClass == category)
            && (type == null || move.type == type)
            && (classification == null || classification in move.classifications)
            && (target == null || move.target in target.showdownTargets)
            && (priority == null || move.priority == priority)
            && (pp == null || move.pp == pp)
            && (power == null || move.basePower in power.range)
            && (accuracy == null || accuracy.matches(move.accuracy))
}

/**
 * The moves whose English or French name, or English description, contains the query (ignoring the case, accents and
 * punctuation), all of them for no query
 */
fun List<PokedexMove>.matching(query: String): List<PokedexMove> {
    val folded = foldName(query)
    if (folded.isEmpty()) return this
    return filter { move ->
        listOfNotNull(move.name.en, move.name.fr, move.shortDesc, move.desc).any { folded in foldName(it) }
    }
}

enum class PokedexMoveSortCriterion(
    override val label: String,
    override val descendingLabel: String,
    override val ascendingLabel: String,
    override val defaultDescending: Boolean,
) : SortCriterion {
    NAME("Name", "Z → A", "A → Z", defaultDescending = false),
    POWER("Power", "Strongest first", "Weakest first", defaultDescending = true),
    ACCURACY("Accuracy", "Most accurate first", "Least accurate first", defaultDescending = true),
    PRIORITY("Priority", "Highest first", "Lowest first", defaultDescending = true),
    PP("PP", "Most first", "Fewest first", defaultDescending = true),
}

data class PokedexMoveSort(
    val criterion: PokedexMoveSortCriterion = PokedexMoveSortCriterion.NAME,
    val descending: Boolean = criterion.defaultDescending,
) {
    val isDefault get() = this == PokedexMoveSort()
}

/** The moves in the order of a sort, the ones equal by the criterion by name */
fun List<PokedexMove>.sortedBy(sort: PokedexMoveSort): List<PokedexMove> {
    val byName = compareBy<PokedexMove> { it.name.en }
    val byCriterion: Comparator<PokedexMove> = when (sort.criterion) {
        PokedexMoveSortCriterion.NAME -> byName
        PokedexMoveSortCriterion.POWER -> compareBy { it.basePower }
        // a move that can't miss is the most accurate
        PokedexMoveSortCriterion.ACCURACY -> compareBy { it.accuracy ?: Int.MAX_VALUE }
        PokedexMoveSortCriterion.PRIORITY -> compareBy { it.priority }
        PokedexMoveSortCriterion.PP -> compareBy { it.pp }
    }
    return sortedWith((if (sort.descending) byCriterion.reversed() else byCriterion).then(byName))
}
