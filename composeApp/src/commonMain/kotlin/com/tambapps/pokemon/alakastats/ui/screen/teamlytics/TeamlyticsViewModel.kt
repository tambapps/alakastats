package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.navigator.Navigator
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.usecase.TeamlyticsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

class TeamlyticsViewModel(
    private val useCase: TeamlyticsUseCase
) : ScreenModel {

    private val scope = CoroutineScope(Dispatchers.Default)
    val teamState = mutableStateOf<Teamlytics?>(null)
    var team: Teamlytics? by teamState
        private set

    fun requireTeam() = team!!

    fun initTeam(id: Uuid, navigator: Navigator) {
        scope.launch {
            val teamlytics = useCase.get(id)
            if (teamlytics == null) {
                navigator.pop()
                return@launch
            }
            team = teamlytics
        }
    }


}