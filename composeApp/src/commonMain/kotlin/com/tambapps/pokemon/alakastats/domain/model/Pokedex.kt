package com.tambapps.pokemon.alakastats.domain.model

import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.Stat

/** A name in the languages of the Pokédex */
data class LocalizedName(val en: String, val fr: String)

/** The entries of the Pokédex, which can be searched by name */
sealed interface PokedexEntry {
    /** The id of Showdown, e.g. `charizardmegay` */
    val id: String
    val name: LocalizedName
}

data class PokedexType(val type: PokeType, override val name: LocalizedName) : PokedexEntry {
    override val id get() = type.name
}

/**
 * @property championsModified whether Champions changed what it does compared to the main games
 * @property descFr null when PokéAPI has none, or only the one of the main games for a modified ability
 */
data class PokedexAbility(
    override val id: String,
    override val name: LocalizedName,
    val shortDesc: String?,
    val desc: String?,
    val descFr: String?,
    val championsModified: Boolean,
) : PokedexEntry

/**
 * @property accuracy null for a move that can't miss
 * @property target Showdown's target, e.g. `allAdjacentFoes`
 * @property championsModified whether Champions changed what it does compared to the main games
 * @property descFr null when PokéAPI has none, or only the one of the main games for a modified move
 */
data class PokedexMove(
    override val id: String,
    override val name: LocalizedName,
    val type: PokeType,
    val damageClass: DamageClass,
    val basePower: Int,
    val accuracy: Int?,
    val pp: Int,
    val priority: Int,
    val target: String,
    val makesContact: Boolean,
    val shortDesc: String?,
    val desc: String?,
    val descFr: String?,
    val championsModified: Boolean,
) : PokedexEntry

/**
 * @property megaForms the ids of the Megas a Mega Stone makes mega evolve
 */
data class PokedexItem(
    override val id: String,
    override val name: LocalizedName,
    val megaForms: List<String>,
    val shortDesc: String?,
    val desc: String?,
    val descFr: String?,
    val championsModified: Boolean,
) : PokedexEntry

/**
 * A Pokémon legal in the Pokédex's format. A Mega is one of its own, its moves being the ones of the form it mega
 * evolves from.
 *
 * @property pokemonName its name in the app, the one of Showdown (e.g. `Charizard-Mega-Y`)
 * @property baseSpeciesId the Pokémon it is a form of (e.g. tauros for Tauros-Paldea-Aqua), null for a base form
 * @property megaOfId the form a Mega mega evolves from
 * @property cosmeticForms the forms only differing from it by their look (e.g. Vivillon-Polar), not entries of their own
 */
data class PokedexSpecies(
    override val id: String,
    override val name: LocalizedName,
    val pokemonName: PokemonName,
    val num: Int,
    val types: List<PokeType>,
    val baseStats: PokeStats,
    val abilities: List<PokedexAbility>,
    val hiddenAbility: PokedexAbility?,
    val baseSpeciesId: String?,
    val isMega: Boolean,
    val megaOfId: String?,
    val megaStone: PokedexItem?,
    val weightKg: Double,
    val cosmeticForms: List<PokemonName>,
    val moves: Set<PokedexMove>,
) : PokedexEntry {
    val allAbilities: List<PokedexAbility> get() = abilities + listOfNotNull(hiddenAbility)
}

/** The base stats a Pokémon must have, both bounds included */
data class StatFilter(val stat: Stat, val min: Int = 0, val max: Int = Int.MAX_VALUE)

/**
 * The criteria a Pokémon must all meet
 *
 * @property types the types it must all have (one or two)
 * @property learns the moves it must all learn
 */
data class PokedexFilter(
    val types: Set<PokeType> = emptySet(),
    val ability: PokedexAbility? = null,
    val learns: Set<PokedexMove> = emptySet(),
    val stats: List<StatFilter> = emptyList(),
    val includeMegas: Boolean = true,
)

/**
 * The Pokémon of a format, the moves, abilities, items and types they refer to, and the queries on them. Everything
 * is in memory: the queries are only lookups and filters on a few hundred Pokémon.
 *
 * @property formatId the format of Showdown it's the Pokédex of, e.g. `gen9championsvgc2026regmc`
 */
class Pokedex(
    val formatId: String,
    val species: List<PokedexSpecies>,
    val moves: List<PokedexMove>,
    val abilities: List<PokedexAbility>,
    val items: List<PokedexItem>,
    val types: List<PokedexType>,
) {
    private val speciesById = species.associateBy { it.id }
    private val movesById = moves.associateBy { it.id }
    private val abilitiesById = abilities.associateBy { it.id }
    private val itemsById = items.associateBy { it.id }

    // a cosmetic form is found as its base form
    private val speciesByName: Map<PokemonName, PokedexSpecies> = buildMap {
        for (s in species) {
            put(s.pokemonName.normalized, s)
            s.cosmeticForms.forEach { put(it.normalized, s) }
        }
    }
    private val speciesByType: Map<PokeType, List<PokedexSpecies>> = species
        .flatMap { s -> s.types.map { it to s } }
        .groupBy({ it.first }, { it.second })
    private val speciesByAbility: Map<String, List<PokedexSpecies>> = species
        .flatMap { s -> s.allAbilities.map { it.id to s } }
        .groupBy({ it.first }, { it.second })
    private val speciesByMove: Map<String, List<PokedexSpecies>> = species
        .flatMap { s -> s.moves.map { it.id to s } }
        .groupBy({ it.first }, { it.second })
    private val megasByForm: Map<String, List<PokedexSpecies>> = species
        .filter { it.megaOfId != null }
        .groupBy { it.megaOfId!! }
    private val nameIndex = PokedexNameIndex(species + moves + abilities + items + types)

    fun species(id: String): PokedexSpecies? = speciesById[id]
    fun move(id: String): PokedexMove? = movesById[id]
    fun ability(id: String): PokedexAbility? = abilitiesById[id]
    fun item(id: String): PokedexItem? = itemsById[id]

    /** The Pokémon of a name of the app (e.g. of a replay), a cosmetic form giving its base form */
    fun species(name: PokemonName): PokedexSpecies? = speciesByName[name.normalized]

    /** A Pokémon and its Megas, the form a Mega mega evolves from for a Mega */
    fun formsOf(species: PokedexSpecies): List<PokedexSpecies> {
        val base = species.megaOfId?.let(speciesById::get) ?: species
        return listOf(base) + megasByForm[base.id].orEmpty()
    }

    /** The Pokémon having a type, or both types when there are two */
    fun withTypes(type: PokeType, otherType: PokeType? = null): List<PokedexSpecies> =
        speciesByType[type].orEmpty().filter { otherType == null || otherType in it.types }

    /** The Pokémon having an ability, hidden or not */
    fun withAbility(ability: PokedexAbility): List<PokedexSpecies> = speciesByAbility[ability.id].orEmpty()

    /** The Pokémon learning a move */
    fun learners(move: PokedexMove): List<PokedexSpecies> = speciesByMove[move.id].orEmpty()

    /** The moves of a Pokémon, by name */
    fun movepool(species: PokedexSpecies): List<PokedexMove> = species.moves.sortedBy { it.name.en }

    /**
     * The Pokémon meeting all the criteria of a filter, e.g. the Water Pokémon learning Trick Room with a base speed
     * below 50
     */
    fun search(
        filter: PokedexFilter,
        sort: Comparator<PokedexSpecies> = compareBy { it.num },
    ): List<PokedexSpecies> {
        // from the smallest of the lists the indexes give, the other criteria filtering it
        val candidates = listOfNotNull(
            filter.learns.map { learners(it) }.minByOrNull { it.size },
            filter.ability?.let { withAbility(it) },
            filter.types.map { speciesByType[it].orEmpty() }.minByOrNull { it.size },
        ).minByOrNull { it.size } ?: species
        return candidates.filter { s ->
            (filter.includeMegas || !s.isMega)
                && s.types.containsAll(filter.types)
                && (filter.ability == null || filter.ability in s.allAbilities)
                && s.moves.containsAll(filter.learns)
                && filter.stats.all { s.baseStats[it.stat] in it.min..it.max }
        }.sortedWith(sort)
    }

    /**
     * The entries whose English or French name matches a query, the best matches first: ignoring the case, accents
     * and punctuation, and tolerating a prefix or typos
     */
    fun searchByName(query: String, limit: Int = 20): List<PokedexEntry> = nameIndex.search(query, limit)
}
