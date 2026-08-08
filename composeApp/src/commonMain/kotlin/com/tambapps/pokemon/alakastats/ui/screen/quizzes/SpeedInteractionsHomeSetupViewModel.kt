package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.navigator.Navigator
import com.tambapps.pokemon.alakastats.domain.model.Format

class SpeedInteractionsHomeSetupViewModel : ScreenModel {
    var format by mutableStateOf(Format.NONE)

    fun selectFormat(newFormat: Format) {
        format = newFormat
    }

    fun startQuiz(navigator: Navigator) {
        navigator.push(SpeedInteractionsQuizScreen(SpeedInteractionsMode.Home(format)))
    }
}
