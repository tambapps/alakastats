package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import androidx.lifecycle.ViewModel
import com.tambapps.pokemon.alakastats.domain.model.QuizType

class QuizzesViewModel : ViewModel() {
    val quizzes: List<QuizType> = QuizType.entries
}
