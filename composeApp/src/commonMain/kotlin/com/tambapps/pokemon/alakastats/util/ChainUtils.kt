package com.tambapps.pokemon.alakastats.util

/**
 * Inserts the items at the right spot of a chain of battles, in case they were unordered: right after the item whose
 * next reference is theirs, or right before the item they reference as next, or at the end
 */
fun <T> MutableList<T>.insertInChain(items: List<T>, reference: (T) -> String, nextReference: (T) -> String?) {
    for (item in items) {
        val previousIndex = indexOfFirst { nextReference(it) == reference(item) }
        if (previousIndex >= 0) {
            add(previousIndex + 1, item)
            continue
        }
        val nextIndex = indexOfFirst { reference(it) == nextReference(item) }
        if (nextIndex >= 0) {
            add(nextIndex, item)
            continue
        }
        add(item)
    }
}
