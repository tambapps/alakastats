package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import arrow.core.flatMap
import cafe.adriel.voyager.core.model.ScreenModel
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.domain.model.usesLegacySystem
import com.tambapps.pokemon.alakastats.domain.repository.FormatDataRepository
import com.tambapps.pokemon.alakastats.domain.repository.PokemonBaseStatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

data class SpeedQuestion(
    val pokemonName: PokemonName,
    val holdsScarf: Boolean,
    val correctSpeed: Int
)

data class SpeedQuizResult(
    val question: SpeedQuestion,
    val guess: Int?,
    val outcome: AnswerOutcome
)

class SpeedStatQuizViewModel(
    private val format: Format,
    private val allowChoiceScarf: Boolean,
    val useMaxStatPoints: Boolean,
    val useSpeedBoostingNature: Boolean,
    private val formatRepository: FormatDataRepository,
    private val pokemonBaseStatsRepository: PokemonBaseStatsRepository
) : ScreenModel {

    var isLoading by mutableStateOf(true)
        private set
    var loadFailed by mutableStateOf(false)
        private set

    var questions: List<SpeedQuestion> = emptyList()
        private set

    var currentIndex by mutableStateOf(0)
        private set

    var guess by mutableStateOf("")

    var lastOutcome by mutableStateOf<AnswerOutcome?>(null)
        private set

    private val _results = mutableListOf<SpeedQuizResult>()
    val results: List<SpeedQuizResult> get() = _results

    private val scope = CoroutineScope(Dispatchers.Main)

    val currentQuestion: SpeedQuestion? get() = questions.getOrNull(currentIndex)
    val isFinished: Boolean get() = questions.isNotEmpty() && currentIndex >= questions.size
    val isLastQuestion: Boolean get() = currentIndex == questions.lastIndex
    val correctCount: Int get() = _results.count { it.outcome == AnswerOutcome.CORRECT }
    val canSubmit: Boolean get() = guess.toIntOrNull() != null

    init {
        loadQuestions()
    }

    private fun loadQuestions() {
        isLoading = true
        loadFailed = false
        scope.launch {
            formatRepository.get(format)
                .flatMap { formatData ->
                    pokemonBaseStatsRepository.getBaseStats(formatData.popularPokemons)
                        .map { statsByPokemon -> formatData.popularPokemons.shuffled() to statsByPokemon }
                }
                .fold(
                    ifLeft = {
                        withContext(Dispatchers.Main) {
                            isLoading = false
                            loadFailed = true
                        }
                    },
                    ifRight = { (shuffledPokemons, statsByPokemon) ->
                        val evs = PokeStats.default(if (useMaxStatPoints) 32 else 0)
                        val nature = if (useSpeedBoostingNature) Nature.JOLLY else Nature.QUIRKY
                        val legacySystem = format.usesLegacySystem(evs)
                        val level = format.pokemonLevel ?: 50

                        val generatedQuestions = shuffledPokemons.mapNotNull { pokemonName ->
                            val baseStats = statsByPokemon[pokemonName.normalized] ?: return@mapNotNull null
                            if (baseStats.speed <= 0) return@mapNotNull null
                            val holdsScarf = allowChoiceScarf && Random.nextBoolean()
                            val speed = PokeStats.compute(
                                baseStats = baseStats,
                                evs = evs,
                                nature = nature,
                                level = level,
                                legacySystem = legacySystem
                            ).speed.let { if (holdsScarf) (it * 1.5f).toInt() else it }
                            SpeedQuestion(pokemonName, holdsScarf, speed)
                        }

                        withContext(Dispatchers.Main) {
                            isLoading = false
                            loadFailed = generatedQuestions.isEmpty()
                            questions = generatedQuestions
                        }
                    }
                )
        }
    }

    fun submit() {
        val question = currentQuestion ?: return
        if (lastOutcome != null || !canSubmit) return
        val guessedValue = guess.toIntOrNull() ?: return
        val outcome = if (guessedValue == question.correctSpeed) AnswerOutcome.CORRECT else AnswerOutcome.INCORRECT
        lastOutcome = outcome
        _results.add(SpeedQuizResult(question, guessedValue, outcome))
    }

    fun pass() {
        val question = currentQuestion ?: return
        if (lastOutcome != null) return
        lastOutcome = AnswerOutcome.PASSED
        _results.add(SpeedQuizResult(question, null, AnswerOutcome.PASSED))
    }

    fun nextQuestion() {
        currentIndex++
        guess = ""
        lastOutcome = null
    }

    fun restart() {
        currentIndex = 0
        guess = ""
        lastOutcome = null
        _results.clear()
    }
}
