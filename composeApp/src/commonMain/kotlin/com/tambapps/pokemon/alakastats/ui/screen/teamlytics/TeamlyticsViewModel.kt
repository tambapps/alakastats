package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import arrow.core.getOrElse
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.navigator.Navigator
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.usecase.HandleTeamReplaysUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.TeamlyticsUseCase
import com.tambapps.pokemon.alakastats.infrastructure.service.ReplayAnalyticsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

class TeamlyticsViewModel(
    private val useCase: TeamlyticsUseCase,
    private val replayService: ReplayAnalyticsService
) : ScreenModel, HandleTeamReplaysUseCase {

    private val scope = CoroutineScope(Dispatchers.Default)
    val teamState = mutableStateOf<Teamlytics?>(null)
    var team: Teamlytics? by teamState
        private set

    fun requireTeam() = team!!

    fun initTeam(id: Uuid, navigator: Navigator) {
        scope.launch {
            val teamlyticsResult = useCase.get(id)
            val teamlytics = teamlyticsResult.getOrElse { 
                navigator.pop()
                return@launch
            }
            team = teamlytics
        }
    }

    override suspend fun parseReplay(url: String) = replayService.fetch(url)

    override suspend fun addReplays(replays: List<ReplayAnalytics>) {
        val team = this.team!!
        val updatedTeam = useCase.save(team.copy(replays = team.replays + replays))
        withContext(Dispatchers.Main) {
            teamState.value = updatedTeam
        }
    }
}