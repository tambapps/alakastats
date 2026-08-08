package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import arrow.core.flatMap
import cafe.adriel.voyager.core.model.ScreenModel
import com.tambapps.pokemon.ItemName
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.domain.model.NEUTRAL_INVESTMENT_EVS
import com.tambapps.pokemon.alakastats.domain.model.NEUTRAL_INVESTMENT_IVS
import com.tambapps.pokemon.alakastats.domain.model.SPEED_INTERACTIONS_QUIZ_LEVEL
import com.tambapps.pokemon.alakastats.domain.model.SPEED_NEUTRAL_NATURE
import com.tambapps.pokemon.alakastats.domain.model.SpeedComparisonResult
import com.tambapps.pokemon.alakastats.domain.model.SpeedRange
import com.tambapps.pokemon.alakastats.domain.model.computeSpeedRange
import com.tambapps.pokemon.alakastats.domain.model.isHoldingChoiceScarf
import com.tambapps.pokemon.alakastats.domain.model.speedComparisonResultFor
import com.tambapps.pokemon.alakastats.domain.model.usesLegacySystem
import com.tambapps.pokemon.alakastats.domain.repository.FormatDataRepository
import com.tambapps.pokemon.alakastats.domain.repository.PokemonBaseStatsRepository
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

sealed interface SpeedInteractionsMode {
    data class Home(val format: Format) : SpeedInteractionsMode
    data class Team(val teamId: Uuid) : SpeedInteractionsMode
}

data class SpeedInteractionQuestion(
    val pokemonA: PokemonName,
    val pokemonB: PokemonName,
    val itemA: ItemName?,
    val speedA: Int,
    // Home mode
    val speedB: Int?,
    // Team mode
    val range: SpeedRange?,
    val correctResult: SpeedComparisonResult
)

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
                is SpeedInteractionsMode.Team -> loadTeamQuestions(m.teamId)
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
                        SpeedInteractionQuestion(nameA, nameB, null, speedA, speedB, null, result)
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

    private suspend fun loadTeamQuestions(teamId: Uuid) {
        val team = teamlyticsRepository.get(teamId).fold(ifLeft = { null }, ifRight = { it })
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
        val teamPokemons = team.pokePaste.pokemons
        formatRepository.get(format)
            .flatMap { formatData ->
                pokemonBaseStatsRepository.getBaseStats(formatData.popularPokemons + teamPokemons.map { it.name })
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
                    val generatedQuestions = teamPokemons.shuffled().mapNotNull { pokemon ->
                        val baseStatsA = statsByPokemon[pokemon.name.normalized] ?: return@mapNotNull null
                        if (baseStatsA.speed <= 0 || validOpponents.isEmpty()) return@mapNotNull null
                        val pokemonB = validOpponents.random()
                        val baseStatsB = statsByPokemon.getValue(pokemonB.normalized)
                        val legacySystem = format.usesLegacySystem(pokemon.evs)
                        val speedA = PokeStats.compute(
                            baseStats = baseStatsA,
                            evs = pokemon.evs,
                            ivs = pokemon.ivs,
                            nature = pokemon.nature ?: SPEED_NEUTRAL_NATURE,
                            level = SPEED_INTERACTIONS_QUIZ_LEVEL,
                            legacySystem = legacySystem
                        ).speed.let { if (isHoldingChoiceScarf(pokemon.item)) (it * 1.5f).toInt() else it }
                        val range = computeSpeedRange(baseStatsB, legacySystem)
                        val result = speedComparisonResultFor(speedA, range)
                        SpeedInteractionQuestion(pokemon.name, pokemonB, pokemon.item, speedA, null, range, result)
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
