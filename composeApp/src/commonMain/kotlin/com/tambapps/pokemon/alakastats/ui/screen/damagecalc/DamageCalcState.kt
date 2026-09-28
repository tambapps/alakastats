package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.AbilityName
import com.tambapps.pokemon.ItemName
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.Stat
import com.tambapps.pokemon.champions.data.ChampionsDex
import com.tambapps.pokemon.champions.engine.BattleFormat
import com.tambapps.pokemon.champions.engine.BattlePokemon
import com.tambapps.pokemon.champions.engine.Battlefield
import com.tambapps.pokemon.champions.engine.StatBoosts
import com.tambapps.pokemon.champions.engine.Status
import com.tambapps.pokemon.champions.engine.Terrain
import com.tambapps.pokemon.champions.engine.Weather

const val MAX_STAT_POINTS_PER_STAT = 32
const val MAX_TOTAL_STAT_POINTS = 66
const val MAX_BOOST_STAGE = 6
const val MAX_MOVES = 4

// neutral natures all have the same effect, only keep one
val NATURES = Nature.entries.filter { !it.isNeutral || it == Nature.SERIOUS }

val STATS = listOf(Stat.HP, Stat.ATTACK, Stat.DEFENSE, Stat.SPECIAL_ATTACK, Stat.SPECIAL_DEFENSE, Stat.SPEED)

// Hail doesn't exist in Champions, it was replaced by Snow
val WEATHERS = listOf(Weather.NONE, Weather.SUN, Weather.RAIN, Weather.SAND, Weather.SNOW)

enum class DamageCalcSide(val displayName: String, val keyStats: List<Stat>) {
    ATTACKER("Attacker", listOf(Stat.ATTACK, Stat.SPECIAL_ATTACK)),
    DEFENDER("Defender", listOf(Stat.HP, Stat.DEFENSE, Stat.SPECIAL_DEFENSE)),
}

val Status.displayName get() = when (this) {
    Status.HEALTHY -> "Healthy"
    Status.BURNED -> "Burned"
    Status.PARALYZED -> "Paralyzed"
    Status.POISONED -> "Poisoned"
    Status.BADLY_POISONED -> "Badly Poisoned"
    Status.ASLEEP -> "Asleep"
    Status.FROZEN -> "Frozen"
}

val BattleFormat.displayName get() = when (this) {
    BattleFormat.SINGLES -> "Singles"
    BattleFormat.DOUBLES -> "Doubles"
}

val Weather.displayName get() = when (this) {
    Weather.NONE -> "None"
    Weather.SUN -> "Sun"
    Weather.RAIN -> "Rain"
    Weather.SAND -> "Sand"
    Weather.HAIL -> "Hail"
    Weather.SNOW -> "Snow"
}

val Terrain.displayName get() = when (this) {
    Terrain.NONE -> "None"
    Terrain.ELECTRIC -> "Electric"
    Terrain.GRASSY -> "Grassy"
    Terrain.MISTY -> "Misty"
    Terrain.PSYCHIC -> "Psychic"
}

val Battlefield.summary get() = listOfNotNull(
    format.displayName,
    if (weather == Weather.NONE) "No weather" else weather.displayName,
    if (terrain == Terrain.NONE) null else "${terrain.displayName} Terrain",
    if (isGravity) "Gravity" else null,
    if (isFairyAura) "Fairy Aura" else null,
    if (isCharge) "Charge" else null,
).joinToString(" · ")

/**
 * The editable state of a pokemon in the damage calc. Kept separate from the engine's [BattlePokemon]
 * as it can be in states the engine can't represent (e.g. a species unknown to Champions), and holds
 * UI-only data (moves, HP as a percentage). See [toBattlePokemon].
 */
class DamageCalcPokemonState(
    name: PokemonName,
    // TODO dummy values, will be fetched later
    ability: String,
    item: String,
    moves: List<String>,
) {
    var name by mutableStateOf(name)
    var nature by mutableStateOf(Nature.SERIOUS)
    var ability by mutableStateOf(ability)
    var item by mutableStateOf(item)
    var status by mutableStateOf(Status.HEALTHY)
    var moves by mutableStateOf(moves)
    var currentHpPercent by mutableStateOf(100)
    private val statPoints = mutableStateMapOf<Stat, Int>().apply { STATS.forEach { put(it, 0) } }
    private val boosts = mutableStateMapOf<Stat, Int>()

    val totalStatPoints get() = statPoints.values.sum()
    // can be negative, the max total is not enforced
    val remainingStatPoints get() = MAX_TOTAL_STAT_POINTS - totalStatPoints
    val exceedsMaxTotalStatPoints get() = remainingStatPoints < 0

    /**
     * Select a species, using its default ability if it is known to Champions
     */
    fun selectSpecies(pokemonName: PokemonName) {
        name = pokemonName
        ChampionsDex.speciesOrNull(pokemonName)?.let { ability = it.defaultAbility.pretty }
    }

    /**
     * Fill this state with the set of a pokemon (e.g. from a team)
     */
    fun fillFrom(pokemon: Pokemon) {
        name = pokemon.name
        ability = pokemon.ability?.pretty ?: ""
        item = pokemon.item?.pretty ?: ""
        // only one neutral nature is proposed
        nature = pokemon.nature?.takeUnless { it.isNeutral } ?: Nature.SERIOUS
        STATS.forEach { setStatPoints(it, pokemon.evs.get(it)) }
        // always keep MAX_MOVES slots so that missing moves can be filled
        val teamMoves = pokemon.moves.take(MAX_MOVES).map { it.pretty }
        moves = teamMoves + List(MAX_MOVES - teamMoves.size) { "" }
    }

    fun setMove(index: Int, move: String) {
        moves = moves.toMutableList().also { it[index] = move }
    }

    fun getBoost(stat: Stat) = boosts[stat] ?: 0

    fun setBoost(stat: Stat, value: Int) {
        boosts[stat] = value.coerceIn(-MAX_BOOST_STAGE, MAX_BOOST_STAGE)
    }

    fun getStatPoints(stat: Stat) = statPoints[stat] ?: 0

    fun setStatPoints(stat: Stat, value: Int) {
        statPoints[stat] = value.coerceIn(0, MAX_STAT_POINTS_PER_STAT)
    }

    /**
     * Convert this state to the damage engine's representation, or null if the species isn't known to Champions
     */
    fun toBattlePokemon(): BattlePokemon? {
        val species = ChampionsDex.speciesOrNull(name) ?: return null
        val battlePokemon = BattlePokemon(
            species = species,
            ability = AbilityName(ability),
            nature = nature,
            statPoints = PokeStats(
                hp = getStatPoints(Stat.HP),
                speed = getStatPoints(Stat.SPEED),
                attack = getStatPoints(Stat.ATTACK),
                specialAttack = getStatPoints(Stat.SPECIAL_ATTACK),
                defense = getStatPoints(Stat.DEFENSE),
                specialDefense = getStatPoints(Stat.SPECIAL_DEFENSE),
            ),
            item = item.takeIf { it.isNotBlank() }?.let(::ItemName),
            boosts = StatBoosts(
                attack = getBoost(Stat.ATTACK),
                defense = getBoost(Stat.DEFENSE),
                specialAttack = getBoost(Stat.SPECIAL_ATTACK),
                specialDefense = getBoost(Stat.SPECIAL_DEFENSE),
                speed = getBoost(Stat.SPEED),
            ),
            status = status,
        )
        if (currentHpPercent >= 100) return battlePokemon
        // the engine wants HP points, which depend on the stats computed from the species
        return battlePokemon.copy(currentHp = (battlePokemon.maxHp * currentHpPercent / 100).coerceAtLeast(1))
    }
}
