package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.ui.navigation.AppNavigator
import com.tambapps.pokemon.alakastats.ui.navigation.AppRoute

class SpeedStatQuizSetupViewModel : ViewModel() {
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

    fun startQuiz(navigator: AppNavigator) {
        navigator.push(AppRoute.SpeedStatQuiz(format, allowChoiceScarf, useMaxStatPoints, useSpeedBoostingNature))
    }
}
