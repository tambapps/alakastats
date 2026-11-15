package com.tambapps.pokemon.alakastats.infrastructure.repository

import com.tambapps.pokemon.AbilityName
import com.tambapps.pokemon.ItemName
import com.tambapps.pokemon.MoveName
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.PresetSet
import com.tambapps.pokemon.alakastats.domain.model.PresetSets
import com.tambapps.pokemon.champions.data.ChampionsDex
import kotlinx.serialization.Serializable

@Serializable
data class PresetSetStatPointsEntity(
    val hp: Int = 0,
    val atk: Int = 0,
    val def: Int = 0,
    val spa: Int = 0,
    val spd: Int = 0,
    val spe: Int = 0,
)

/**
 * A set of files/sets/champions.json, written by ncp_sets_to_json.mcl
 *
 * @param ability empty for the species' default ability
 */
@Serializable
data class PresetSetEntity(
    val name: String,
    val nature: String,
    val ability: String = "",
    val item: String = "",
    val statPoints: PresetSetStatPointsEntity = PresetSetStatPointsEntity(),
    val moves: List<String> = emptyList(),
)

/** The sets of each species, by its name */
typealias PresetSetsEntity = Map<String, List<PresetSetEntity>>

/**
 * The sets that can be used: a set with an unknown nature is left out, rather than the whole file
 */
internal fun PresetSetsEntity.toDomain() = PresetSets(
    flatMap { (species, sets) -> sets.mapNotNull { it.toDomain(PokemonName(species)) } }
)

internal fun PresetSetEntity.toDomain(species: PokemonName): PresetSet? {
    val nature = Nature.entries.find { it.name.equals(nature, ignoreCase = true) } ?: return null
    return PresetSet(
        name = name,
        species = species,
        nature = nature,
        // like the source calculator, a set without ability has the species' default one
        ability = ability.ifBlank { null }?.let(::AbilityName) ?: ChampionsDex.speciesOrNull(species)?.defaultAbility,
        item = item.ifBlank { null }?.let(::ItemName),
        statPoints = PokeStats(
            hp = statPoints.hp,
            attack = statPoints.atk,
            defense = statPoints.def,
            specialAttack = statPoints.spa,
            specialDefense = statPoints.spd,
            speed = statPoints.spe,
        ),
        moves = moves.map(::MoveName),
    )
}
