package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.ui.navigation.AppNavigator
import com.tambapps.pokemon.alakastats.ui.navigation.AppRoute

class SpeedInteractionsHomeSetupViewModel : ViewModel() {
    var format by mutableStateOf(Format.NONE)

    fun selectFormat(newFormat: Format) {
        format = newFormat
    }

    fun startQuiz(navigator: AppNavigator) {
        navigator.push(AppRoute.SpeedInteractionsQuiz(SpeedInteractionsMode.Home(format)))
    }
}
