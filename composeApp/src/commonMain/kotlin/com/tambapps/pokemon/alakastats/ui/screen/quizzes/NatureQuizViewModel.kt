package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.Stat

enum class AnswerOutcome {
    CORRECT, INCORRECT, PASSED
}

enum class QuizDirection(val displayName: String) {
    NAME_TO_STATS("Nature → Stats"),
    STATS_TO_NAME("Stats → Nature"),
}

data class NatureQuizResult(val nature: Nature, val outcome: AnswerOutcome)

class NatureQuizViewModel(natures: List<Nature>, val direction: QuizDirection) : ScreenModel {
    val questions: List<Nature> = natures.shuffled()

    var currentIndex by mutableStateOf(0)
        private set

    var guess by mutableStateOf("")

    var selectedIncreasedStat by mutableStateOf<Stat?>(null)
        private set
    var selectedDecreasedStat by mutableStateOf<Stat?>(null)
        private set
    var hasPickedIncreasedStat by mutableStateOf(false)
        private set
    var hasPickedDecreasedStat by mutableStateOf(false)
        private set

    var lastOutcome by mutableStateOf<AnswerOutcome?>(null)
        private set

    private val _results = mutableListOf<NatureQuizResult>()
    val results: List<NatureQuizResult> get() = _results

    val currentNature: Nature? get() = questions.getOrNull(currentIndex)
    val isFinished: Boolean get() = currentIndex >= questions.size
    val isLastQuestion: Boolean get() = currentIndex == questions.lastIndex
    val correctCount: Int get() = _results.count { it.outcome == AnswerOutcome.CORRECT }

    val canSubmit: Boolean
        get() = when (direction) {
            QuizDirection.NAME_TO_STATS -> hasPickedIncreasedStat && hasPickedDecreasedStat
            QuizDirection.STATS_TO_NAME -> guess.isNotBlank()
        }

    fun pickIncreasedStat(stat: Stat?) {
        selectedIncreasedStat = stat
        hasPickedIncreasedStat = true
    }

    fun pickDecreasedStat(stat: Stat?) {
        selectedDecreasedStat = stat
        hasPickedDecreasedStat = true
    }

    fun submit() {
        val nature = currentNature ?: return
        if (lastOutcome != null || !canSubmit) return
        val correct = when (direction) {
            QuizDirection.NAME_TO_STATS -> if (nature.isNeutral) {
                // a neutral nature has no real boosted/lowered stat: accept "None/None"
                // as well as picking the same stat on both sides (net effect cancels out)
                selectedIncreasedStat == selectedDecreasedStat
            } else {
                selectedIncreasedStat == nature.bonusStat && selectedDecreasedStat == nature.malusStat
            }
            QuizDirection.STATS_TO_NAME -> normalize(guess) == normalize(nature.displayName)
        }
        val outcome = if (correct) AnswerOutcome.CORRECT else AnswerOutcome.INCORRECT
        lastOutcome = outcome
        _results.add(NatureQuizResult(nature, outcome))
    }

    fun pass() {
        val nature = currentNature ?: return
        if (lastOutcome != null) return
        lastOutcome = AnswerOutcome.PASSED
        _results.add(NatureQuizResult(nature, AnswerOutcome.PASSED))
    }

    fun nextQuestion() {
        currentIndex++
        resetAnswerState()
    }

    fun restart() {
        currentIndex = 0
        resetAnswerState()
        _results.clear()
    }

    private fun resetAnswerState() {
        guess = ""
        selectedIncreasedStat = null
        selectedDecreasedStat = null
        hasPickedIncreasedStat = false
        hasPickedDecreasedStat = false
        lastOutcome = null
    }

    private fun normalize(s: String) = s.trim().replace(" ", "").lowercase()
}
