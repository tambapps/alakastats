package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail.tabs.speedscale

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import arrow.core.Either
import arrow.core.flatMap
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.Stat
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.domain.model.PokemonData
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.usesLegacySystem
import com.tambapps.pokemon.alakastats.domain.repository.FormatDataRepository
import com.tambapps.pokemon.alakastats.infrastructure.repository.PokeApiPokemonDataRepository
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail.tabs.PokemonDetailTabViewModel
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PokemonSpeed(
    val pokemonName: PokemonName,
    val value: Int,
    val boostNature: Boolean,
    val ev: Int,
    val isPokemonOfInterest: Boolean = false,
    val statsNotFound: Boolean = false
)

data class SpeedScale(
    val interestPokemon: PokemonSpeed,
    // grouped by speed value
    val speedGroups: List<List<PokemonSpeed>>
)
class PokemonSpeedScaleViewModel(
    override val pokemonImageService: PokemonImageService,
    val team: Teamlytics,
    originalPokemon: Pokemon,
    megaPokemon: PokemonName?,
    megaSelected: Boolean,
    val pokemonData: PokemonData?,
    private val pokeApi: PokeApiPokemonDataRepository,
    private val formatRepository: FormatDataRepository
) : PokemonDetailTabViewModel() {
    val pokemon: Pokemon = if (megaPokemon != null && megaSelected) originalPokemon.copy(name = megaPokemon) else originalPokemon
    val usesLegacySystem = team.usesLegacySystem
    val maxEvsValue = if (usesLegacySystem) 252 else 32

    override var isTabLoading by mutableStateOf(false)
    var maxEvs by mutableStateOf(true)
        private set
    var speedNature by mutableStateOf(false)
        private set
    var scarfBoost by mutableStateOf(false)
        private set
    var stage by mutableStateOf(0)
        private set
    var ownScarfBoost by mutableStateOf(pokemon.isScarfOrBooster(pokemonData))
        private set
    var ownStage by mutableStateOf(0)
        private set

    private val scope = CoroutineScope(Dispatchers.Main)
    private var pokemons by mutableStateOf(emptyMap<PokemonName, PokeStats>())
    private val pokemonLevel = pokemon.level

    // grouped by speed value
    var speedScale by mutableStateOf<SpeedScale?>(null)
        private set

    fun loadSpeedScale(onError: ((DomainError) -> Unit)? = null) {
        if (isTabLoading) return
        val format = team.format.takeIf { it != Format.NONE } ?: return
        isTabLoading = true
        scope.launch {
            when {
                pokemons.isNotEmpty() -> Either.Right(pokemons)
                else -> formatRepository.get(format)
                    .flatMap { pokeApi.getBaseStats(it.popularPokemons) }
            }.fold(
                    ifLeft = {
                        withContext(Dispatchers.Main) {
                            isTabLoading = false
                            onError?.invoke(it)
                        }
                    },
                    ifRight = { resultPokemons ->
                        val scale = computeScale(resultPokemons)
                        withContext(Dispatchers.Main) {
                            isTabLoading = false
                            pokemons = resultPokemons
                            speedScale = scale
                        }
                    }
                )
        }
    }

    private fun computeScale(pokemons: Map<PokemonName, PokeStats>): SpeedScale {
        val pokemonBaseStats = pokemons[pokemon.name.normalized] ?: PokeStats.default(0)
        val pokemonSpeed = PokeStats.compute(
            baseStats = pokemonBaseStats,
            evs = pokemon.evs,
            nature = pokemon.nature ?: Nature.QUIRKY,
            level = pokemonLevel,
            legacySystem = usesLegacySystem
        ).speed.let { if (ownScarfBoost) (it * 1.5f).toInt() else it }.toStage(ownStage)
        val interestPokemon = PokemonSpeed(
            pokemonName = pokemon.name,
            value = pokemonSpeed,
            boostNature = pokemon.nature?.bonusStat == Stat.SPEED,
            ev = pokemon.evs.speed,
            isPokemonOfInterest = true
        )

        val pokemonSpeeds = buildList {
            pokemons.forEach { (pokeName, baseStats) ->
                val speed = PokeStats.compute(
                    baseStats,
                    evs = PokeStats.default(if (maxEvs) maxEvsValue else 0),
                    nature = if (speedNature) Nature.JOLLY else Nature.QUIRKY,
                    level = pokemonLevel,
                    legacySystem = usesLegacySystem
                ).speed.let { if (scarfBoost) (it * 1.5f).toInt()  else it }.toStage(stage)
                val statsNotFound = baseStats.speed == 0
                add(PokemonSpeed(pokeName, speed, speedNature, 0, statsNotFound = statsNotFound))
            }
            // adding last in purpose, because the interestPokemon will be looking left
            add(interestPokemon)
        }
        val speedGroups = pokemonSpeeds.groupBy { it.value }
            .asSequence()
            .sortedBy { - it.key }
            .map { it.value }
            .toList()

        return SpeedScale(
            interestPokemon = interestPokemon,
            speedGroups = speedGroups
        )
    }

    fun flipMaxEvs() {
        if (isTabLoading) return
        this.maxEvs = !maxEvs
        loadSpeedScale()
    }

    fun flipSpeedNature() {
        if (isTabLoading) return
        this.speedNature = !speedNature
        loadSpeedScale()
    }
    fun flipScarfBoostNature() {
        if (isTabLoading) return
        this.scarfBoost = !scarfBoost
        loadSpeedScale()
    }

    fun flipOwnScarfBoostNature() {
        if (isTabLoading) return
        this.ownScarfBoost = !ownScarfBoost
        loadSpeedScale()
    }

    fun updateStage(value: Int) {
        if (isTabLoading) return
        this.stage = value
        loadSpeedScale()
    }

    fun updateOwnStage(value: Int) {
        if (isTabLoading) return
        this.ownStage = value
        loadSpeedScale()
    }

    private fun Pokemon.isScarfOrBooster(pokemonData: PokemonData?): Boolean {
        val item = item ?: return false
        val baseStats = pokemonData?.baseStatsOf(name)
        return item.normalized.value.let {
            it == "choice-scarf"
                    || it == "booster-energy" && baseStats != null && PokeStats.compute(this, baseStats, legacySystem = usesLegacySystem).isSpeedHighestStat() }
    }

}

fun Int.toStage(level: Int): Int {
    if (level == 0) return this
    val (num, denom) = if (level >= 0) 2f + level to 2f
    else 2f to (2f - level)

    return (this * num / denom).toInt()
}

private fun PokeStats.isSpeedHighestStat(): Boolean = (Stat.entries - Stat.HP).maxOfOrNull { this[it] } == speed
