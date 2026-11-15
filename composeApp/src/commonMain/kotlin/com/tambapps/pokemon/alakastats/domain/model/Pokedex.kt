package com.tambapps.pokemon.alakastats.domain.model

import com.tambapps.pokemon.AbilityName
import com.tambapps.pokemon.ItemName
import com.tambapps.pokemon.MoveName
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.PokemonNormalizer
import com.tambapps.pokemon.Stat

/** A name in the languages of the Pokédex */
data class LocalizedName(val en: String, val fr: String)

/** The entries of the Pokédex, which can be searched by name */
sealed interface PokedexEntry {
    /**
     * The English name normalized like the app's names are (see [PokemonNormalizer.normalize]), e.g. `charizard-mega-y`,
     * `trick-room`, `king's-rock`
     */
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
) : PokedexEntry {
    val abilityName get() = AbilityName(name.en)
}

/**
 * A family of moves some abilities or items act on: most are Showdown's flags (e.g. the punching moves Iron Fist boosts),
 * the explosive ones are the moves Damp prevents and the mental ones the moves Aroma Veil protects from
 */
enum class MoveClassification {
    PUNCHING, SOUND, DANCE, SLICING, WIND, POWDER, BALL_AND_BOMB, PULSE, BITING, EXPLOSIVE, MENTAL, HEALING,
}

/**
 * @property accuracy null for a move that can't miss
 * @property pp its PP in battle, Champions' (e.g. 8 for a move of 5 PP in the main games)
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
    val classifications: Set<MoveClassification>,
    val shortDesc: String?,
    val desc: String?,
    val descFr: String?,
    val championsModified: Boolean,
) : PokedexEntry {
    val moveName get() = MoveName(name.en)
}

/**
 * @property megaForms the Megas a Mega Stone makes mega evolve, normalized
 */
data class PokedexItem(
    override val id: String,
    override val name: LocalizedName,
    val megaForms: List<PokemonName>,
    val shortDesc: String?,
    val desc: String?,
    val descFr: String?,
    val championsModified: Boolean,
) : PokedexEntry {
    val itemName get() = ItemName(name.en)
}

/**
 * A Pokémon legal in the Pokédex's format. A Mega is one of its own, its moves being the ones of the form it mega
 * evolves from.
 *
 * @property form the form in its name (e.g. Paldea-Aqua for Tauros-Paldea-Aqua, Mega-Y), null for a base form
 * @property baseSpecies the Pokémon it is a form of (e.g. tauros for Tauros-Paldea-Aqua), normalized, null for a base form
 * @property megaOf the form a Mega mega evolves from, normalized
 * @property cosmeticForms the forms only differing from it by their look (e.g. Vivillon-Polar), not entries of their own
 */
data class PokedexSpecies(
    override val id: String,
    override val name: LocalizedName,
    val num: Int,
    val form: String?,
    val types: List<PokeType>,
    val baseStats: PokeStats,
    val abilities: List<PokedexAbility>,
    val hiddenAbility: PokedexAbility?,
    val baseSpecies: PokemonName?,
    val isMega: Boolean,
    val megaOf: PokemonName?,
    val megaStone: PokedexItem?,
    val weightKg: Double,
    val cosmeticForms: List<PokemonName>,
    val moves: Set<PokedexMove>,
) : PokedexEntry {
    /** Its name in the app, the one of Showdown (e.g. `Charizard-Mega-Y`) */
    val pokemonName get() = PokemonName(name.en)

    /**
     * Its English name to show: a form's without dashes (e.g. Tauros Paldea Aqua, Mega Absol Z, see
     * [PokemonName.pretty]), a base form's as it is (e.g. Kommo-o, Mr. Mime)
     */
    val displayName: String get() = if (form == null) name.en else pokemonName.pretty

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
    // a cosmetic form is found as its base form
    private val speciesByName = NameLookup(species) { s -> listOf(s.pokemonName.value) + s.cosmeticForms.map { it.value } }
    private val movesByName = NameLookup(moves)
    private val abilitiesByName = NameLookup(abilities)
    private val itemsByName = NameLookup(items)

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
        .mapNotNull { mega -> mega.megaOf?.let { it.value to mega } }
        .groupBy({ it.first }, { it.second })
    private val nameIndex = PokedexNameIndex(species + moves + abilities + items + types)

    /**
     * The Pokémon of a name of the app (e.g. of a replay or a paste), a cosmetic form giving its base form. See
     * [NameLookup] for how it's looked up
     */
    fun species(name: PokemonName): PokedexSpecies? = speciesByName.find(name.value)
    fun move(name: MoveName): PokedexMove? = movesByName.find(name.value)
    fun ability(name: AbilityName): PokedexAbility? = abilitiesByName.find(name.value)
    fun item(name: ItemName): PokedexItem? = itemsByName.find(name.value)

    /** A Pokémon and its Megas, the form a Mega mega evolves from for a Mega */
    fun formsOf(pokemon: PokedexSpecies): List<PokedexSpecies> {
        val base = pokemon.megaOf?.let { species(it) } ?: pokemon
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

/**
 * The entries of a table by name: the name normalized like the app's names are first (its id), and else folded
 * (see [foldName]), for a name written otherwise to be found too (e.g. `King’s Shield` with a curly apostrophe, `Kings
 * Shield`, `Flabebe`), like Showdown's ids are
 *
 * @param names the names of an entry, its English one by default
 */
private class NameLookup<T : PokedexEntry>(
    entries: List<T>,
    names: (T) -> List<String> = { listOf(it.name.en) },
) {
    private val byNormalizedName = HashMap<String, T>()
    private val byFoldedName = HashMap<String, T>()

    init {
        for (entry in entries) {
            for (name in names(entry)) {
                byNormalizedName[PokemonNormalizer.normalize(name)] = entry
                byFoldedName[foldName(name)] = entry
            }
        }
    }

    fun find(name: String): T? = byNormalizedName[PokemonNormalizer.normalize(name)] ?: byFoldedName[foldName(name)]
}
