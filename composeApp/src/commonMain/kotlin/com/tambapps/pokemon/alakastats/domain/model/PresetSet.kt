package com.tambapps.pokemon.alakastats.domain.model

import com.tambapps.pokemon.AbilityName
import com.tambapps.pokemon.ItemName
import com.tambapps.pokemon.MoveName
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.PokemonName

private const val CHAMPIONS_LEVEL = 50
private const val DEFAULT_HAPPINESS = 255
private const val MAX_IV = 31

/**
 * A set proposed for a pokemon in the damage calc (e.g. one of the Nerd of Now calculator's sets)
 *
 * @param species the base species, a set holding a mega stone not being a mega form
 * @param ability null if unknown, the species' default ability being used
 */
data class PresetSet(
    val name: String,
    val species: PokemonName,
    val nature: Nature,
    val ability: AbilityName?,
    val item: ItemName?,
    val statPoints: PokeStats,
    val moves: List<MoveName>,
) {

    /**
     * The set as a pokepaste's pokemon, with its stat points as EVs, like the team's pokemon are
     */
    fun toPokemon() = Pokemon(
        name = species,
        surname = null,
        gender = null,
        nature = nature,
        item = item,
        shiny = false,
        happiness = DEFAULT_HAPPINESS,
        ability = ability,
        teraType = null,
        level = CHAMPIONS_LEVEL,
        moves = moves,
        ivs = PokeStats.default(MAX_IV),
        evs = statPoints,
    )
}

/**
 * The preset sets of each species, in their source's order
 */
class PresetSets(sets: List<PresetSet>) {

    private val bySpecies: Map<String, List<PresetSet>> = sets.groupBy { it.species.normalized.value }

    val species: Collection<PokemonName> get() = bySpecies.values.map { it.first().species }

    /** The sets of [species] (not one of its other forms), empty if it has none */
    fun of(species: PokemonName): List<PresetSet> = bySpecies[species.normalized.value].orEmpty()

    companion object {
        val EMPTY = PresetSets(emptyList())
    }
}
