package com.tambapps.pokemon.alakastats.ui.model

import com.tambapps.pokemon.alakastats.domain.model.PokedexSpecies

enum class PokedexPokemonSortCriterion(
    override val label: String,
    override val descendingLabel: String,
    override val ascendingLabel: String,
    override val defaultDescending: Boolean,
) : SortCriterion {
    NAME("Name", "Z → A", "A → Z", defaultDescending = false),
    SPEED("Speed", "Fastest first", "Slowest first", defaultDescending = true),
}

data class PokedexPokemonSort(
    val criterion: PokedexPokemonSortCriterion = PokedexPokemonSortCriterion.NAME,
    val descending: Boolean = criterion.defaultDescending,
) {
    val isDefault get() = this == PokedexPokemonSort()
}

/** The Pokémon in the order of a sort, the ones of the same speed by name */
fun List<PokedexSpecies>.sortedBy(sort: PokedexPokemonSort): List<PokedexSpecies> {
    val byName = compareBy<PokedexSpecies> { it.name.en }
    val comparator = when (sort.criterion) {
        PokedexPokemonSortCriterion.NAME -> if (sort.descending) byName.reversed() else byName
        PokedexPokemonSortCriterion.SPEED -> {
            val bySpeed = compareBy<PokedexSpecies> { it.baseStats.speed }
            (if (sort.descending) bySpeed.reversed() else bySpeed).then(byName)
        }
    }
    return sortedWith(comparator)
}
