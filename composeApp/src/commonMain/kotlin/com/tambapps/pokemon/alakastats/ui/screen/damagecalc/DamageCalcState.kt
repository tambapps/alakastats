package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.AbilityName
import com.tambapps.pokemon.ItemName
import com.tambapps.pokemon.MoveName
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.Stat
import com.tambapps.pokemon.champions.data.Ability
import com.tambapps.pokemon.champions.data.ChampionsDex
import com.tambapps.pokemon.champions.data.Move
import com.tambapps.pokemon.champions.engine.defaultHitCount
import com.tambapps.pokemon.champions.engine.hasActiveToggle
import com.tambapps.pokemon.champions.engine.hitCountRange
import com.tambapps.pokemon.champions.engine.isActiveByDefault
import com.tambapps.pokemon.champions.engine.MoveUse
import com.tambapps.pokemon.champions.engine.returnsDefenderMove
import com.tambapps.pokemon.champions.engine.BattleFormat
import com.tambapps.pokemon.champions.engine.BattlePokemon
import com.tambapps.pokemon.champions.engine.Battlefield
import com.tambapps.pokemon.champions.engine.SpeedCalculator
import com.tambapps.pokemon.champions.engine.StatBoosts
import com.tambapps.pokemon.champions.engine.Status
import com.tambapps.pokemon.champions.engine.Terrain
import com.tambapps.pokemon.champions.engine.Weather

const val MAX_STAT_POINTS_PER_STAT = 32
const val MAX_TOTAL_STAT_POINTS = 66
const val MAX_BOOST_STAGE = 6
const val MAX_MOVES = 4
// the toxic counters the source calculator offers for a badly poisoned pokemon, 1/16 to 15/16
val TOXIC_COUNTERS = 1..15

// neutral natures all have the same effect, only keep one
val NATURES = Nature.entries.filter { !it.isNeutral || it == Nature.SERIOUS }

val STATS = listOf(Stat.HP, Stat.ATTACK, Stat.DEFENSE, Stat.SPECIAL_ATTACK, Stat.SPECIAL_DEFENSE, Stat.SPEED)

/**
 * The stat points matching [evs], the inverse of the source calculator's display of stat points as EVs
 * (max(0, 8 * stat points - 4)): 252 EVs are 32 stat points, 4 EVs are 1
 */
internal fun evsToStatPoints(evs: Int) = ((evs + 4) / 8).coerceIn(0, MAX_STAT_POINTS_PER_STAT)

// how many times Last Respects/Rage Fist's effect can have stacked, like the source calculator's "0x effect" to "6x effect"
val STACK_COUNTS = 0..6
// Supreme Overlord's fainted allies, like the source calculator's "0 down" to "5 down"
val FAINTED_ALLY_COUNTS = 0..5

/**
 * The source calculator's Rivalry setting, relating the Rivalry pokemon's gender to its target's
 */
enum class RivalryRelation(val displayName: String) {
    OFF("Off"),
    SAME("Same Gender"),
    OPPOSITE("Opposite Genders"),
}

// Hail doesn't exist in Champions, it was replaced by Snow
val WEATHERS = listOf(Weather.NONE, Weather.SUN, Weather.RAIN, Weather.SAND, Weather.SNOW)

enum class DamageCalcSide(val displayName: String, val keyStats: List<Stat>) {
    ATTACKER("Attacker", listOf(Stat.ATTACK, Stat.SPECIAL_ATTACK)),
    DEFENDER("Defender", listOf(Stat.HP, Stat.DEFENSE, Stat.SPECIAL_DEFENSE));

    val opponent get() = if (this == ATTACKER) DEFENDER else ATTACKER
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

// what the pokemon picker offers: every Champions pokemon, its forms being picked separately
val PICKABLE_POKEMON_NAMES: List<PokemonName> by lazy {
    ChampionsDex.pickableSpecies.map { it.name }.sortedBy { it.value }
}

/**
 * The forms a pokemon can be in (e.g. Charizard, Mega Charizard X, Mega Charizard Y), or just itself if it
 * has a single form or isn't known to Champions
 */
fun formsOf(pokemonName: PokemonName): List<PokemonName> =
    ChampionsDex.speciesOrNull(pokemonName)?.let { species -> ChampionsDex.formsOf(species).map { it.name } }
        ?: listOf(pokemonName)

val Battlefield.summary get() = listOfNotNull(
    format.displayName,
    if (weather == Weather.NONE) "No weather" else weather.displayName,
    if (terrain == Terrain.NONE) null else "${terrain.displayName} Terrain",
    if (isGravity) "Gravity" else null,
    if (isFairyAura) "Fairy Aura" else null,
).joinToString(" · ")

/**
 * The editable state of a pokemon in the damage calc. Kept separate from the engine's [BattlePokemon]
 * as it can be in states the engine can't represent (e.g. a species unknown to Champions), and holds
 * UI-only data (moves, HP as a percentage). See [toBattlePokemon].
 */
class DamageCalcPokemonState(
    name: PokemonName,
    ability: String,
    item: String,
    moves: List<String>,
) {
    // the pokemon picked, e.g. Charizard
    var name by mutableStateOf(name)
        private set
    // the form of the pokemon used for the calc, e.g. Mega Charizard X. Same as name for single-form pokemons
    var form by mutableStateOf(formsOf(name).first())
        private set
    var nature by mutableStateOf(Nature.SERIOUS)
    private var abilityState by mutableStateOf(ability)
    private var isAbilityActiveState by mutableStateOf(Ability.from(AbilityName(ability)).isActiveByDefault)
    // notified when the ability (true) or only its toggle (false) changes, e.g. to sync what it sets on the field
    internal var onAbilityChange: ((pokemon: DamageCalcPokemonState, isAbilityChange: Boolean) -> Unit)? = null

    /**
     * Changing the ability resets its "active" toggle to the source calculator's default (e.g. on for Intimidate)
     */
    var ability: String
        get() = abilityState
        set(value) {
            abilityState = value
            // like the source calculator, the ability's settings are reset when it changes
            isAbilityActiveState = resolvedAbility.isActiveByDefault
            rivalry = RivalryRelation.OFF
            faintedAllyCount = 0
            onAbilityChange?.invoke(this, true)
        }

    /**
     * The source calculator's "ability on" toggle, only relevant for abilities that have one (see [hasAbilityToggle])
     */
    var isAbilityActive: Boolean
        get() = isAbilityActiveState
        set(value) {
            isAbilityActiveState = value
            onAbilityChange?.invoke(this, false)
        }

    val resolvedAbility: Ability get() = Ability.from(AbilityName(abilityState))
    val hasAbilityToggle get() = resolvedAbility.hasActiveToggle
    var item by mutableStateOf(item)
    var status by mutableStateOf(Status.HEALTHY)
    // the toxic damage of the next end of turn in 16ths of the max HP, only used when badly poisoned
    var toxicCounter by mutableStateOf(1)
    var moves by mutableStateOf(moves)
    /**
     * Like the source calculator, the current HP is both a percentage (100 by default) and HP points. HP points set
     * exactly stay as long as the max HP doesn't change, otherwise they're recomputed from the percentage
     */
    val currentHpPercent: Int get() = currentHpPercentState
    // not a private setter of currentHpPercent, whose JVM name would clash with setCurrentHpPercent()
    private var currentHpPercentState by mutableStateOf(100)
    // the HP points set exactly, and the max HP they were set for
    private var exactCurrentHp by mutableStateOf<ExactHp?>(null)
    private val statPoints = mutableStateMapOf<Stat, Int>().apply { STATS.forEach { put(it, 0) } }
    private val boosts = mutableStateMapOf<Stat, Int>()
    // hit counts explicitly selected, by move index. Moves without one use the engine's default
    private val selectedHitCounts = mutableStateMapOf<Int, Int>()
    // indexes of the moves calculated as critical hits
    private val criticalMoveIndexes = mutableStateMapOf<Int, Boolean>()
    // indexes of the moves whose power doubling condition is met (the source's "2x BP"), for Payback-like moves
    private val powerDoubledMoveIndexes = mutableStateMapOf<Int, Boolean>()
    // how many times Last Respects/Rage Fist's effect already stacked, by move index
    private val stackCounts = mutableStateMapOf<Int, Int>()
    // the index of the defender's move returned by Counter-like moves, by move index
    private val counteredMoveIndexes = mutableStateMapOf<Int, Int>()

    // the source's Rivalry setting, only relevant with the Rivalry ability
    var rivalry by mutableStateOf(RivalryRelation.OFF)
    // Supreme Overlord: how many allies already fainted, only relevant with the Supreme Overlord ability
    var faintedAllyCount by mutableStateOf(0)

    val totalStatPoints get() = statPoints.values.sum()
    // can be negative, the max total is not enforced
    val remainingStatPoints get() = MAX_TOTAL_STAT_POINTS - totalStatPoints
    val exceedsMaxTotalStatPoints get() = remainingStatPoints < 0

    // the forms the picked pokemon can be in
    val availableForms get() = formsOf(name)

    /**
     * Select a pokemon, in its first form (e.g. Charizard, Aegislash-Shield)
     */
    fun selectSpecies(pokemonName: PokemonName) {
        name = pokemonName
        abilityBeforeMega = null
        selectForm(availableForms.first())
        // like the source calculator, a new pokemon is at full HP
        resetCurrentHp()
    }

    /**
     * Select a form of the picked pokemon, using its default ability. Selecting a mega also makes the
     * pokemon hold the mega stone needed to mega evolve.
     */
    fun selectForm(formName: PokemonName) {
        val previousCurrentHp = currentHp
        val previousMaxHp = maxHp
        form = formName
        // like the source calculator, a form change keeps the HP missing from the max HP
        val newMaxHp = maxHp
        if (previousCurrentHp != null && previousMaxHp != null && newMaxHp != null && newMaxHp != previousMaxHp) {
            setCurrentHp(maxOf(0, previousCurrentHp + newMaxHp - previousMaxHp))
        }
        val species = ChampionsDex.speciesOrNull(formName) ?: return
        ability = species.defaultAbility.pretty
        species.megaStone?.let { item = it.pretty }
    }

    /**
     * The mega form the held item makes this pokemon mega evolve into, if its only other forms are megas (e.g. not
     * Rotom), null otherwise. Its form is then a mega switch, not a form selection
     */
    val megaFormOfItem: PokemonName?
        get() {
            // the megas are the forms with a mega stone (named like "Mega Charizard X", which PokemonName.isMega misses)
            val otherFormStones = availableForms.drop(1).map { it to ChampionsDex.speciesOrNull(it)?.megaStone }
            if (otherFormStones.isEmpty() || otherFormStones.any { it.second == null } || item.isBlank()) return null
            return otherFormStones.firstOrNull { (_, stone) -> stone?.matches(ItemName(item)) == true }?.first
        }

    val isMegaEvolved: Boolean get() = megaFormOfItem?.let { form == it } == true

    // the ability before mega evolving, given back when going back to the base form (e.g. the team's one)
    private var abilityBeforeMega: String? = null

    /**
     * Mega evolve into [megaFormOfItem], or go back to the base form with the ability it had before
     */
    fun setMegaEvolved(megaEvolved: Boolean) {
        val megaForm = megaFormOfItem ?: return
        if (megaEvolved == isMegaEvolved) return
        if (megaEvolved) {
            abilityBeforeMega = ability
            selectForm(megaForm)
        } else {
            selectForm(availableForms.first())
            abilityBeforeMega?.let { ability = it }
            abilityBeforeMega = null
        }
    }

    /**
     * The max HP, or null if the species isn't known to Champions
     */
    val maxHp: Int? get() = toFullHpBattlePokemon()?.maxHp

    /**
     * The current HP points, or null if the species isn't known to Champions: the ones set exactly if the max HP
     * didn't change since, else computed from the percentage like the source calculator (rounded up)
     */
    val currentHp: Int?
        get() {
            val max = maxHp ?: return null
            exactCurrentHp?.takeIf { it.maxHp == max }?.let { return it.hp }
            return (currentHpPercent * max + 99) / 100
        }

    /**
     * Set the current HP points exactly (0 to the max HP). The percentage follows, rounded down like the source calculator
     */
    fun setCurrentHp(hp: Int) {
        val max = maxHp ?: return
        val value = hp.coerceIn(0, max)
        exactCurrentHp = ExactHp(value, max)
        currentHpPercentState = 100 * value / max
    }

    /**
     * Set the current HP as a percentage (0 to 100), the HP points being computed from it
     */
    fun setCurrentHpPercent(percent: Int) {
        currentHpPercentState = percent.coerceIn(0, 100)
        exactCurrentHp = null
    }

    private fun resetCurrentHp() = setCurrentHpPercent(100)

    /**
     * Fill this state with the set of a pokemon (e.g. from a team or a pokepaste). A set with EVs (some above the max
     * stat points) has them converted to stat points
     */
    fun fillFrom(pokemon: Pokemon) {
        name = pokemon.name
        abilityBeforeMega = null
        form = availableForms.first()
        ability = pokemon.ability?.pretty ?: ""
        item = pokemon.item?.pretty ?: ""
        nature = natureOf(pokemon)
        STATS.forEach { setStatPoints(it, statPointsOf(pokemon, it)) }
        // always keep MAX_MOVES slots so that missing moves can be filled
        val teamMoves = pokemon.moves.take(MAX_MOVES).map { it.pretty }
        moves = teamMoves + List(MAX_MOVES - teamMoves.size) { "" }
        selectedHitCounts.clear()
        criticalMoveIndexes.clear()
        powerDoubledMoveIndexes.clear()
        stackCounts.clear()
        counteredMoveIndexes.clear()
        resetCurrentHp()
    }

    /**
     * Whether this pokemon has the set [fillFrom] fills from [pokemon]: same species (not a mega form), ability, item,
     * nature and stat points. Moves and battle settings (boosts, status, HP...) don't matter
     */
    fun hasSetOf(pokemon: Pokemon): Boolean =
        name.normalized == pokemon.name.normalized &&
            form == availableForms.first() &&
            ability == (pokemon.ability?.pretty ?: "") &&
            item == (pokemon.item?.pretty ?: "") &&
            nature == natureOf(pokemon) &&
            STATS.all { getStatPoints(it) == statPointsOf(pokemon, it) }

    // only one neutral nature is proposed
    private fun natureOf(pokemon: Pokemon) = pokemon.nature?.takeUnless { it.isNeutral } ?: Nature.SERIOUS

    // a set with EVs (some above the max stat points) has them converted
    private fun statPointsOf(pokemon: Pokemon, stat: Stat): Int {
        val value = pokemon.evs.get(stat)
        val hasEvs = pokemon.evs.any { it > MAX_STAT_POINTS_PER_STAT }
        return (if (hasEvs) evsToStatPoints(value) else value).coerceIn(0, MAX_STAT_POINTS_PER_STAT)
    }

    /**
     * Change the move at [index]. Like the source calculator, its crit toggle, stack count and returned move are reset
     * (the "2x BP" toggle is kept, it only applies to moves that can double)
     */
    fun setMove(index: Int, move: String) {
        moves = moves.toMutableList().also { it[index] = move }
        selectedHitCounts.remove(index)
        criticalMoveIndexes.remove(index)
        stackCounts.remove(index)
        counteredMoveIndexes.remove(index)
    }

    /**
     * Whether the power doubling condition of the move at [index] is met (Payback, Avalanche...), false for a move
     * that can't double
     */
    fun isPowerDoubled(index: Int) =
        championsMove(index)?.canBePowerDoubled == true && powerDoubledMoveIndexes[index] == true

    fun setPowerDoubled(index: Int, isPowerDoubled: Boolean) {
        powerDoubledMoveIndexes[index] = isPowerDoubled
    }

    /**
     * How many times the effect of the move at [index] already stacked (Last Respects, Rage Fist), 0 for other moves
     */
    fun stackCount(index: Int) = if (championsMove(index)?.hasStackingPower == true) stackCounts[index] ?: 0 else 0

    fun setStackCount(index: Int, count: Int) {
        stackCounts[index] = count.coerceIn(STACK_COUNTS.first, STACK_COUNTS.last)
    }

    /**
     * The index of the defender's move returned by the Counter-like move at [index] (the first one by default)
     */
    fun counteredMoveIndex(index: Int) = counteredMoveIndexes[index] ?: 0

    fun setCounteredMoveIndex(index: Int, defenderMoveIndex: Int) {
        counteredMoveIndexes[index] = defenderMoveIndex
    }

    /**
     * The engine's use of the move at [index], with this pokemon's settings for it, or null if the slot is empty or
     * the move unknown to Champions
     *
     * @param counteredMove the defender's move, returned if this move is a Counter-like one
     */
    fun moveUse(index: Int, counteredMove: MoveUse? = null): MoveUse? {
        val move = championsMove(index) ?: return null
        return MoveUse(
            move = move,
            isCritical = isCritical(index),
            isPowerDoubled = isPowerDoubled(index),
            priorPowerBoosts = stackCount(index),
            faintedAllyCount = if (resolvedAbility == Ability.SUPREME_OVERLORD) faintedAllyCount else 0,
            counteredMove = if (move.returnsDefenderMove) counteredMove else null,
        )
    }

    /**
     * The Champions move at [index], or null if the slot is empty or the move unknown to Champions
     */
    fun championsMove(index: Int): Move? =
        moves.getOrNull(index)?.takeIf { it.isNotBlank() }?.let { ChampionsDex.moveOrNull(MoveName(it)) }

    /**
     * The hit counts that can be selected for the move at [index], or null if it can't vary (e.g. a single hit move)
     */
    fun selectableHitCounts(index: Int): IntRange? =
        championsMove(index)?.hitCountRange?.takeIf { it.first != it.last }

    /**
     * The number of hits considered for the move at [index]: the one selected, or else the default one
     * (e.g. 3 for a 2-5 hit move, 5 with Skill Link)
     */
    fun hitCount(index: Int): Int {
        val move = championsMove(index) ?: return 1
        selectedHitCounts[index]?.let { return it }
        return toBattlePokemon()?.let { defaultHitCount(move, it) } ?: move.hitCountRange.first
    }

    fun selectHitCount(index: Int, hits: Int) {
        selectedHitCounts[index] = hits
    }

    /**
     * Whether the move at [index] always lands a critical hit (e.g. Wicked Blow), whatever the crit toggle
     */
    fun alwaysCrits(index: Int) = championsMove(index)?.alwaysCrits == true

    /**
     * Whether the move at [index] is calculated as a critical hit
     */
    fun isCritical(index: Int) = alwaysCrits(index) || criticalMoveIndexes[index] == true

    fun setCritical(index: Int, isCritical: Boolean) {
        criticalMoveIndexes[index] = isCritical
    }

    fun getBoost(stat: Stat) = boosts[stat] ?: 0

    fun setBoost(stat: Stat, value: Int) {
        boosts[stat] = value.coerceIn(-MAX_BOOST_STAGE, MAX_BOOST_STAGE)
    }

    /**
     * The final stats (base stats, stat points and nature, before boosts) computed by the engine,
     * or null if the species isn't known to Champions
     */
    val stats: PokeStats? get() = toBattlePokemon()?.stats

    /**
     * The speed the calc uses for this pokemon on [side] of [field] (boosts, Choice Scarf, Tailwind, paralysis, weather
     * abilities...), or null if the species isn't known to Champions
     */
    fun finalSpeed(field: Battlefield, side: DamageCalcSide): Int? =
        toBattlePokemon()?.let { SpeedCalculator.effectiveSpeed(it, field, isAttackerSide = side == DamageCalcSide.ATTACKER) }

    fun getStatPoints(stat: Stat) = statPoints[stat] ?: 0

    fun setStatPoints(stat: Stat, value: Int) {
        statPoints[stat] = value.coerceIn(0, MAX_STAT_POINTS_PER_STAT)
    }

    /**
     * Convert this state to the damage engine's representation, or null if the species isn't known to Champions
     */
    fun toBattlePokemon(): BattlePokemon? {
        val battlePokemon = toFullHpBattlePokemon() ?: return null
        return battlePokemon.copy(currentHp = currentHp)
    }

    // the engine's representation at full HP, from which the max HP is computed
    private fun toFullHpBattlePokemon(): BattlePokemon? {
        val species = ChampionsDex.speciesOrNull(form) ?: return null
        return BattlePokemon(
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
            toxicCounter = toxicCounter,
            abilityIsActive = isAbilityActive,
        )
    }
}

private data class ExactHp(val hp: Int, val maxHp: Int)
