package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import arrow.core.getOrElse
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.navigator.Navigator
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsNotes
import com.tambapps.pokemon.alakastats.domain.usecase.HandleTeamOverviewUseCase
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
) : ScreenModel, HandleTeamReplaysUseCase, HandleTeamOverviewUseCase {

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
        val team = requireTeam()
        save(team.copy(replays = trySetElo(team.replays + replays)))
    }

    override suspend fun setNotes(team: Teamlytics, notes: TeamlyticsNotes?) {
        val team = requireTeam()
        save(team.copy(notes = notes))
    }

    override fun export(team: Teamlytics) = useCase.export(team)

    private fun trySetElo(replays: List<ReplayAnalytics>) = replays.map { replay ->
        if (replay.player1.beforeElo != null) return@map replay
        val next = findNextMatchWithElo(replay, replays) ?: return@map replay

        return@map replay.copy(
            players = replay.players.mapIndexed { index, player ->
                val nextPlayer = next.players.getOrNull(index) ?: player
                player.copy(
                    beforeElo = nextPlayer.beforeElo
                )
            }
        )
    }

    private fun findNextMatchWithElo(replay: ReplayAnalytics, replays: List<ReplayAnalytics>): ReplayAnalytics? {
        val visitedReplays = mutableSetOf<ReplayAnalytics>()
        var r: ReplayAnalytics = replay

        while (r.nextBattleRef != null && !visitedReplays.contains(r)) {
            visitedReplays.add(r)
            val next = replays.find { it.reference == r.nextBattleRef } ?: return null
            if (next.player1.beforeElo != null) {
                return next
            }
            r = next
        }
        return null
    }

    override suspend fun removeReplay(replay: ReplayAnalytics) {
        val team = requireTeam()
        save(team.copy(replays = team.replays - replay))
    }

    override suspend fun replaceReplay(original: ReplayAnalytics, replay: ReplayAnalytics) {
        val team = requireTeam()
        val replayIndex = team.replays.indexOf(original)
        val replays = team.replays.mapIndexed { index, r ->
            if (index == replayIndex) replay else r
        }
        save(team.copy(replays = replays))
    }

    private suspend fun save(team: Teamlytics) {
        val updatedTeam = useCase.save(team)
        withContext(Dispatchers.Main) {
            teamState.value = updatedTeam
        }
    }
}