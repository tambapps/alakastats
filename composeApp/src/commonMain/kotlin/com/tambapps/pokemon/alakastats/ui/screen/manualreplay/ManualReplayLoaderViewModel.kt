package com.tambapps.pokemon.alakastats.ui.screen.manualreplay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.domain.model.FormatData
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.repository.FormatDataRepository
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import kotlin.uuid.Uuid

sealed interface ManualReplayLoadState {
    data object Loading : ManualReplayLoadState
    data class Loaded(
        val team: Teamlytics,
        val formatData: FormatData?,
        val replayToEdit: ReplayAnalytics?,
    ) : ManualReplayLoadState
    data class Failed(val message: String) : ManualReplayLoadState
}

/**
 * Finds what the manual replay screen needs from what its route tells, ids: a route has to be saved with the back stack
 *
 * @param replayReference the manual replay to edit, null to create one
 */
class ManualReplayLoaderViewModel(
    private val teamId: Uuid,
    private val replayReference: String?,
    private val teamlyticsRepository: TeamlyticsRepository,
    private val formatDataRepository: FormatDataRepository,
) : ViewModel() {

    var state by mutableStateOf<ManualReplayLoadState>(ManualReplayLoadState.Loading)
        private set

    suspend fun load() {
        if (state != ManualReplayLoadState.Loading) return
        state = teamlyticsRepository.get(teamId).fold(
            ifLeft = { ManualReplayLoadState.Failed("Error: ${it.message}") },
            ifRight = { team -> loaded(team) },
        )
    }

    private suspend fun loaded(team: Teamlytics): ManualReplayLoadState {
        val replayToEdit = replayReference?.let { reference ->
            team.replays.find { it.reference == reference }
                ?: return ManualReplayLoadState.Failed("The replay could not be found")
        }
        val formatData = team.format
            .takeIf { it != Format.NONE }
            ?.let { formatDataRepository.get(it) }
            ?.getOrNull()
        return ManualReplayLoadState.Loaded(team, formatData, replayToEdit)
    }
}
