package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.Stat

const val MAX_STAT_POINTS_PER_STAT = 32
const val MAX_TOTAL_STAT_POINTS = 66
const val MAX_BOOST_STAGE = 6

// neutral natures all have the same effect, only keep one
val NATURES = Nature.entries.filter { !it.isNeutral || it == Nature.SERIOUS }

val STATS = listOf(Stat.HP, Stat.ATTACK, Stat.DEFENSE, Stat.SPECIAL_ATTACK, Stat.SPECIAL_DEFENSE, Stat.SPEED)

enum class DamageCalcSide(val displayName: String, val keyStats: List<Stat>) {
    ATTACKER("Attacker", listOf(Stat.ATTACK, Stat.SPECIAL_ATTACK)),
    DEFENDER("Defender", listOf(Stat.HP, Stat.DEFENSE, Stat.SPECIAL_DEFENSE)),
}

enum class StatusCondition(val displayName: String) {
    HEALTHY("Healthy"),
    BURNED("Burned"),
    POISONED("Poisoned"),
    BADLY_POISONED("Badly Poisoned"),
    PARALYZED("Paralyzed"),
    ASLEEP("Asleep"),
    FROZEN("Frozen"),
}

enum class BattleFormat(val displayName: String) {
    SINGLES("Singles"),
    DOUBLES("Doubles"),
}

enum class Weather(val displayName: String) {
    NONE("None"),
    SUN("Sun"),
    RAIN("Rain"),
    SAND("Sand"),
    SNOW("Snow"),
}

enum class Terrain(val displayName: String) {
    NONE("None"),
    ELECTRIC("Electric"),
    GRASSY("Grassy"),
    MISTY("Misty"),
    PSYCHIC("Psychic"),
}

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
    var status by mutableStateOf(StatusCondition.HEALTHY)
    var moves by mutableStateOf(moves)
    var currentHpPercent by mutableStateOf(100)
    // conditions of the side of the field this pokemon is on, so that they follow the pokemon on swap
    val sideConditions = SideConditionsState()
    private val statPoints = mutableStateMapOf<Stat, Int>().apply { STATS.forEach { put(it, 0) } }
    private val boosts = mutableStateMapOf<Stat, Int>()

    val totalStatPoints get() = statPoints.values.sum()
    // can be negative, the max total is not enforced
    val remainingStatPoints get() = MAX_TOTAL_STAT_POINTS - totalStatPoints
    val exceedsMaxTotalStatPoints get() = remainingStatPoints < 0

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
}

const val MAX_SPIKES = 3

enum class SideCondition(val displayName: String) {
    PROTECT("Protect"),
    HELPING_HAND("Helping Hand"),
    AURORA_VEIL("Aurora Veil"),
    REFLECT("Reflect"),
    LIGHT_SCREEN("Light Screen"),
    TAILWIND("Tailwind"),
    LEECH_SEED("Leech Seed"),
    FRIEND_GUARD("Friend Guard"),
    STEALTH_ROCK("Stealth Rock"),
    STEELY_SPIRIT("Steely Spirit"),
    SALT_CURE("Salt Cure"),
    INGRAIN("Ingrain"),
    CURSE("Curse"),
    BINDING("Binding"),
    CHARGE("Charge"),
    AQUA_RING("Aqua Ring"),
}

class SideConditionsState {
    private val activeConditions = mutableStateMapOf<SideCondition, Boolean>()
    var spikes by mutableIntStateOf(0)

    fun isActive(condition: SideCondition) = activeConditions[condition] == true

    fun toggle(condition: SideCondition) {
        activeConditions[condition] = !isActive(condition)
    }

    fun cycleSpikes() {
        spikes = (spikes + 1) % (MAX_SPIKES + 1)
    }
}

class FieldState {
    var format by mutableStateOf(BattleFormat.DOUBLES)
    var weather by mutableStateOf(Weather.NONE)
    var terrain by mutableStateOf(Terrain.NONE)
    var gravity by mutableStateOf(false)
    var fairyAura by mutableStateOf(false)

    val summary get() = listOfNotNull(
        format.displayName,
        if (weather == Weather.NONE) "No weather" else weather.displayName,
        if (terrain == Terrain.NONE) null else "${terrain.displayName} Terrain",
        if (gravity) "Gravity" else null,
        if (fairyAura) "Fairy Aura" else null,
    ).joinToString(" · ")
}
