package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import arrow.core.flatMap
import cafe.adriel.voyager.core.model.ScreenModel
import com.tambapps.pokemon.ItemName
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.domain.model.NEUTRAL_INVESTMENT_EVS
import com.tambapps.pokemon.alakastats.domain.model.NEUTRAL_INVESTMENT_IVS
import com.tambapps.pokemon.alakastats.domain.model.SPEED_INTERACTIONS_QUIZ_LEVEL
import com.tambapps.pokemon.alakastats.domain.model.SPEED_NEUTRAL_NATURE
import com.tambapps.pokemon.alakastats.domain.model.OpponentSpeedRange
import com.tambapps.pokemon.alakastats.domain.model.RevealedNature
import com.tambapps.pokemon.alakastats.domain.model.SpeedComparisonResult
import com.tambapps.pokemon.alakastats.domain.model.SpeedModifier
import com.tambapps.pokemon.alakastats.domain.model.applyChoiceScarf
import com.tambapps.pokemon.alakastats.domain.model.applyParalysis
import com.tambapps.pokemon.alakastats.domain.model.applyTailwind
import com.tambapps.pokemon.alakastats.domain.model.computeOpponentSpeedRange
import com.tambapps.pokemon.alakastats.domain.model.isHoldingChoiceScarf
import com.tambapps.pokemon.alakastats.domain.model.minInvestmentToReach
import com.tambapps.pokemon.alakastats.domain.model.speedComparisonResultFor
import com.tambapps.pokemon.alakastats.domain.model.usesLegacySystem
import com.tambapps.pokemon.alakastats.domain.repository.FormatDataRepository
import com.tambapps.pokemon.alakastats.domain.repository.PokemonBaseStatsRepository
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.util.MegaUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random
import kotlin.uuid.Uuid

sealed interface SpeedInteractionsMode {
    data class Home(val format: Format) : SpeedInteractionsMode
    data class Team(
        val teamId: Uuid,
        val allowOpponentScarf: Boolean = false,
        val allowParalysis: Boolean = false,
        val allowTailwind: Boolean = false,
        val includeNonMegaBaseForms: Boolean = false,
        val allowReducingNature: Boolean = false
    ) : SpeedInteractionsMode
}

data class SpeedInteractionQuestion(
    val pokemonA: PokemonName,
    val pokemonB: PokemonName,
    val itemA: ItemName?,
    val speedA: Int,
    // Home mode
    val speedB: Int?,
    // Team mode
    val opponentRange: OpponentSpeedRange?,
    val opponentNature: RevealedNature?,
    val investmentThreshold: Int?,
    val correctResult: SpeedComparisonResult,
    val modifier: SpeedModifier = SpeedModifier.NONE
)

private data class TeamEntry(val pokemon: Pokemon, val resolvedName: PokemonName)

/**
 * A mega-capable member yields either just its mega form (default) or both its base and mega
 * forms when [includeNonMegaBaseForms] is on. Handles both pokepaste conventions: the mega form
 * written directly, or the base form holding a mega stone.
 */
private fun teamEntriesFor(pokemon: Pokemon, includeNonMegaBaseForms: Boolean): List<TeamEntry> {
    val baseName = if (pokemon.name.isMega) pokemon.name.baseNormalized else pokemon.name.normalized
    val megaName = if (pokemon.name.isMega) pokemon.name.normalized else MegaUtils.getMegaPokemon(pokemon.item)
    return when {
        megaName == null -> listOf(TeamEntry(pokemon, baseName))
        includeNonMegaBaseForms -> listOf(TeamEntry(pokemon, baseName), TeamEntry(pokemon, megaName))
        else -> listOf(TeamEntry(pokemon, megaName))
    }
}

data class SpeedInteractionsResult(
    val question: SpeedInteractionQuestion,
    val guess: SpeedComparisonResult?,
    val outcome: AnswerOutcome
)

class SpeedInteractionsViewModel(
    val mode: SpeedInteractionsMode,
    val pokemonImageService: PokemonImageService,
    private val formatRepository: FormatDataRepository,
    private val pokemonBaseStatsRepository: PokemonBaseStatsRepository,
    private val teamlyticsRepository: TeamlyticsRepository
) : ScreenModel {

    var isLoading by mutableStateOf(true)
        private set
    var loadFailed by mutableStateOf(false)
        private set
    var noFormatSet by mutableStateOf(false)
        private set

    var questions: List<SpeedInteractionQuestion> = emptyList()
        private set
    var resolvedFormat by mutableStateOf<Format?>((mode as? SpeedInteractionsMode.Home)?.format)
        private set

    var currentIndex by mutableStateOf(0)
        private set
    var selectedAnswer by mutableStateOf<SpeedComparisonResult?>(null)
        private set
    var lastOutcome by mutableStateOf<AnswerOutcome?>(null)
        private set

    private val _results = mutableListOf<SpeedInteractionsResult>()
    val results: List<SpeedInteractionsResult> get() = _results

    private val scope = CoroutineScope(Dispatchers.Main)

    val isTeamMode: Boolean get() = mode is SpeedInteractionsMode.Team
    val currentQuestion: SpeedInteractionQuestion? get() = questions.getOrNull(currentIndex)
    val isFinished: Boolean get() = questions.isNotEmpty() && currentIndex >= questions.size
    val isLastQuestion: Boolean get() = currentIndex == questions.lastIndex
    val correctCount: Int get() = _results.count { it.outcome == AnswerOutcome.CORRECT }

    init {
        loadQuestions()
    }

    private fun loadQuestions() {
        isLoading = true
        loadFailed = false
        noFormatSet = false
        scope.launch {
            when (val m = mode) {
                is SpeedInteractionsMode.Home -> loadHomeQuestions(m.format)
                is SpeedInteractionsMode.Team -> loadTeamQuestions(m)
            }
        }
    }

    private suspend fun loadHomeQuestions(format: Format) {
        formatRepository.get(format)
            .flatMap { formatData -> pokemonBaseStatsRepository.getBaseStats(formatData.popularPokemons) }
            .fold(
                ifLeft = {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        loadFailed = true
                    }
                },
                ifRight = { statsByPokemon ->
                    val legacySystem = format.usesLegacySystem(NEUTRAL_INVESTMENT_EVS)
                    val validPokemons = statsByPokemon.filterValues { it.speed > 0 }.keys.shuffled()
                    val generatedQuestions = validPokemons.chunked(2).mapNotNull { pair ->
                        if (pair.size < 2) return@mapNotNull null
                        val (nameA, nameB) = pair
                        val speedA = neutralSpeed(statsByPokemon.getValue(nameA), legacySystem)
                        val speedB = neutralSpeed(statsByPokemon.getValue(nameB), legacySystem)
                        val result = when {
                            speedA > speedB -> SpeedComparisonResult.FASTER
                            speedA < speedB -> SpeedComparisonResult.SLOWER
                            else -> SpeedComparisonResult.SPEED_TIE
                        }
                        SpeedInteractionQuestion(
                            pokemonA = nameA,
                            pokemonB = nameB,
                            itemA = null,
                            speedA = speedA,
                            speedB = speedB,
                            opponentRange = null,
                            opponentNature = null,
                            investmentThreshold = null,
                            correctResult = result
                        )
                    }
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        loadFailed = generatedQuestions.isEmpty()
                        questions = generatedQuestions
                    }
                }
            )
    }

    private fun neutralSpeed(baseStats: PokeStats, legacySystem: Boolean) = PokeStats.compute(
        baseStats = baseStats,
        evs = NEUTRAL_INVESTMENT_EVS,
        ivs = NEUTRAL_INVESTMENT_IVS,
        nature = SPEED_NEUTRAL_NATURE,
        level = SPEED_INTERACTIONS_QUIZ_LEVEL,
        legacySystem = legacySystem
    ).speed

    private suspend fun loadTeamQuestions(mode: SpeedInteractionsMode.Team) {
        val team = teamlyticsRepository.get(mode.teamId).fold(ifLeft = { null }, ifRight = { it })
        if (team == null) {
            withContext(Dispatchers.Main) {
                isLoading = false
                loadFailed = true
            }
            return
        }
        val format = team.format
        if (format == Format.NONE) {
            withContext(Dispatchers.Main) {
                isLoading = false
                noFormatSet = true
            }
            return
        }
        withContext(Dispatchers.Main) {
            resolvedFormat = format
        }
        val entries = team.pokePaste.pokemons.flatMap { teamEntriesFor(it, mode.includeNonMegaBaseForms) }
        formatRepository.get(format)
            .flatMap { formatData ->
                pokemonBaseStatsRepository.getBaseStats(formatData.popularPokemons + entries.map { it.resolvedName })
                    .map { statsByPokemon -> formatData.popularPokemons to statsByPokemon }
            }
            .fold(
                ifLeft = {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        loadFailed = true
                    }
                },
                ifRight = { (popularPokemons, statsByPokemon) ->
                    val validOpponents = popularPokemons.filter { (statsByPokemon[it.normalized]?.speed ?: 0) > 0 }
                    val enabledModifiers = buildList {
                        if (mode.allowOpponentScarf) add(SpeedModifier.B_CHOICE_SCARF)
                        if (mode.allowParalysis) {
                            add(SpeedModifier.B_PARALYSIS)
                            add(SpeedModifier.A_PARALYSIS)
                        }
                        if (mode.allowTailwind) add(SpeedModifier.A_TAILWIND)
                    }
                    val naturePool = if (mode.allowReducingNature) {
                        RevealedNature.entries
                    } else {
                        RevealedNature.entries.filter { it != RevealedNature.REDUCING }
                    }
                    val generatedQuestions = entries.shuffled().mapNotNull { entry ->
                        val pokemon = entry.pokemon
                        val baseStatsA = statsByPokemon[entry.resolvedName.normalized] ?: return@mapNotNull null
                        if (baseStatsA.speed <= 0 || validOpponents.isEmpty()) return@mapNotNull null
                        val pokemonB = validOpponents.random()
                        val baseStatsB = statsByPokemon.getValue(pokemonB.normalized)
                        val legacySystem = format.usesLegacySystem(pokemon.evs)
                        var speedA = PokeStats.compute(
                            baseStats = baseStatsA,
                            evs = pokemon.evs,
                            ivs = pokemon.ivs,
                            nature = pokemon.nature ?: SPEED_NEUTRAL_NATURE,
                            level = SPEED_INTERACTIONS_QUIZ_LEVEL,
                            legacySystem = legacySystem
                        ).speed
                        if (isHoldingChoiceScarf(pokemon.item)) {
                            speedA = applyChoiceScarf(speedA)
                        }
                        val revealedNature = naturePool.random()
                        val modifier = if (enabledModifiers.isEmpty() || Random.nextBoolean()) {
                            SpeedModifier.NONE
                        } else {
                            enabledModifiers.random()
                        }
                        val bTransform: (Int) -> Int = when (modifier) {
                            SpeedModifier.B_CHOICE_SCARF -> { s -> applyChoiceScarf(s) }
                            SpeedModifier.B_PARALYSIS -> { s -> applyParalysis(s) }
                            else -> { s -> s }
                        }
                        when (modifier) {
                            SpeedModifier.A_PARALYSIS -> speedA = applyParalysis(speedA)
                            SpeedModifier.A_TAILWIND -> speedA = applyTailwind(speedA)
                            else -> {}
                        }
                        val rawRange = computeOpponentSpeedRange(baseStatsB, revealedNature, legacySystem)
                        val range = OpponentSpeedRange(bTransform(rawRange.min), bTransform(rawRange.max))
                        val result = speedComparisonResultFor(speedA, range)
                        val threshold = if (result == SpeedComparisonResult.DEPENDS_ON_INVESTMENT) {
                            minInvestmentToReach(baseStatsB, revealedNature, legacySystem, speedA, bTransform)
                        } else {
                            null
                        }
                        SpeedInteractionQuestion(
                            pokemonA = entry.resolvedName,
                            pokemonB = pokemonB,
                            itemA = pokemon.item,
                            speedA = speedA,
                            speedB = null,
                            opponentRange = range,
                            opponentNature = revealedNature,
                            investmentThreshold = threshold,
                            correctResult = result,
                            modifier = modifier
                        )
                    }
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        loadFailed = generatedQuestions.isEmpty()
                        questions = generatedQuestions
                    }
                }
            )
    }

    fun submit(answer: SpeedComparisonResult) {
        val question = currentQuestion ?: return
        if (lastOutcome != null) return
        selectedAnswer = answer
        val outcome = if (answer == question.correctResult) AnswerOutcome.CORRECT else AnswerOutcome.INCORRECT
        lastOutcome = outcome
        _results.add(SpeedInteractionsResult(question, answer, outcome))
    }

    fun pass() {
        val question = currentQuestion ?: return
        if (lastOutcome != null) return
        lastOutcome = AnswerOutcome.PASSED
        _results.add(SpeedInteractionsResult(question, null, AnswerOutcome.PASSED))
    }

    fun nextQuestion() {
        currentIndex++
        selectedAnswer = null
        lastOutcome = null
    }

    fun restart() {
        currentIndex = 0
        selectedAnswer = null
        lastOutcome = null
        _results.clear()
    }
}
