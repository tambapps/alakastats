package com.tambapps.pokemon.alakastats.domain.model

import kotlin.math.min

/**
 * The search by name of the Pokédex's entries, in English and French. A query and a name are compared folded: in
 * lowercase, without accents and punctuation ("Méga-Dracaufeu Y" being "megadracaufeuy").
 */
internal class PokedexNameIndex(entries: List<PokedexEntry>) {

    private val keys: List<Pair<String, PokedexEntry>> = entries.flatMap { entry ->
        listOf(entry.name.en, entry.name.fr).map(::foldName).distinct().map { it to entry }
    }

    fun search(query: String, limit: Int): List<PokedexEntry> {
        val folded = foldName(query)
        if (folded.isEmpty()) return emptyList()
        // a short query would match about anything with a typo
        val maxTypos = when {
            folded.length <= 3 -> 0
            folded.length <= 6 -> 1
            else -> 2
        }
        return keys.mapNotNull { (key, entry) -> matchScore(folded, key, maxTypos)?.let { Triple(it, key, entry) } }
            .sortedWith(compareBy({ it.first }, { it.second.length }, { it.second }))
            .map { it.third }
            .distinct()
            .take(limit)
    }

    /** The lower the better, null for no match */
    private fun matchScore(query: String, key: String, maxTypos: Int): Int? = when {
        key == query -> 0
        key.startsWith(query) -> 1
        query in key -> 2
        // a typo in the query, the name possibly going on after it
        maxTypos > 0 && minOf(
            typoDistance(query, key.take(query.length)),
            typoDistance(query, key.take(query.length + 1)),
            typoDistance(query, key.take((query.length - 1).coerceAtLeast(0))),
        ) <= maxTypos -> 3
        else -> null
    }
}

/** A name in lowercase, without accents nor anything but letters and digits */
internal fun foldName(s: String): String = buildString {
    for (c in s.lowercase()) {
        when (c) {
            'à', 'á', 'â', 'ä', 'ã' -> append('a')
            'é', 'è', 'ê', 'ë' -> append('e')
            'î', 'ï', 'í', 'ì' -> append('i')
            'ô', 'ö', 'ó', 'ò', 'õ' -> append('o')
            'ù', 'û', 'ü', 'ú' -> append('u')
            'ç' -> append('c')
            'ñ' -> append('n')
            'ÿ' -> append('y')
            'œ' -> append("oe")
            'æ' -> append("ae")
            '♀' -> append('f')
            '♂' -> append('m')
            else -> if (c in 'a'..'z' || c in '0'..'9') append(c)
        }
    }
}

/** The optimal string alignment distance: the insertions, deletions, substitutions and swaps of two adjacent letters */
internal fun typoDistance(a: String, b: String): Int {
    val d = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) d[i][0] = i
    for (j in 0..b.length) d[0][j] = j
    for (i in 1..a.length) {
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            d[i][j] = min(min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost)
            if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                d[i][j] = min(d[i][j], d[i - 2][j - 2] + 1)
            }
        }
    }
    return d[a.length][b.length]
}
