package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.navigator.Navigator
import com.tambapps.pokemon.alakastats.domain.model.Format

class SpeedStatQuizSetupViewModel : ScreenModel {
    var format by mutableStateOf(Format.NONE)

    var allowChoiceScarf by mutableStateOf(false)
    var useMaxStatPoints by mutableStateOf(false)
    var useSpeedBoostingNature by mutableStateOf(false)

    fun selectFormat(newFormat: Format) {
        format = newFormat
    }

    fun toggleChoiceScarf() {
        allowChoiceScarf = !allowChoiceScarf
    }

    fun toggleMaxStatPoints() {
        useMaxStatPoints = !useMaxStatPoints
    }

    fun toggleSpeedBoostingNature() {
        useSpeedBoostingNature = !useSpeedBoostingNature
    }

    fun startQuiz(navigator: Navigator) {
        navigator.push(SpeedStatQuizScreen(format, allowChoiceScarf, useMaxStatPoints, useSpeedBoostingNature))
    }
}
