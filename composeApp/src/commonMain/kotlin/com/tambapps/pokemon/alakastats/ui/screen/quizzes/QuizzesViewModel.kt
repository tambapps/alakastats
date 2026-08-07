package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import cafe.adriel.voyager.core.model.ScreenModel
import com.tambapps.pokemon.alakastats.domain.model.QuizType

class QuizzesViewModel : ScreenModel {
    val quizzes: List<QuizType> = QuizType.entries
}
