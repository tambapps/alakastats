package com.tambapps.pokemon.alakastats.ui.model

/**
 * Something a list can be sorted by, in both directions
 *
 * @param defaultDescending the natural direction, e.g. the newest first for a date
 */
interface SortCriterion {
    val label: String
    val descendingLabel: String
    val ascendingLabel: String
    val defaultDescending: Boolean
}
