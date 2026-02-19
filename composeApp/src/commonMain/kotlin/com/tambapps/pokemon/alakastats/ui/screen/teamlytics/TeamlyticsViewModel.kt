package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import arrow.core.Either
import arrow.core.raise.either
import cafe.adriel.voyager.core.model.ScreenModel
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.domain.model.MatchupNotes
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsData
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsNotes
import com.tambapps.pokemon.alakastats.domain.model.withComputedElo
import com.tambapps.pokemon.alakastats.domain.model.withContext
import com.tambapps.pokemon.alakastats.domain.usecase.ConsultTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageMatchupNotesUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamOverviewUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamReplaysUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.ui.composables.PagerViewModel
import com.tambapps.pokemon.alakastats.ui.model.ReplayFilters
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

sealed class TeamState {
    data object Loading : TeamState()
    data class Loaded(val team: Teamlytics) : TeamState()
    data class Error(val error: DomainError) : TeamState()
}

class TeamlyticsViewModel(
    private val teamId: Uuid,
    private val useCase: ManageTeamlyticsUseCase,
    val imageService: PokemonImageService,
) : ScreenModel, ConsultTeamlyticsUseCase, ManageTeamReplaysUseCase, ManageTeamOverviewUseCase,
    ManageMatchupNotesUseCase, PagerViewModel {

    private val scope = CoroutineScope(Dispatchers.Default)
    var teamState by mutableStateOf<TeamState>(TeamState.Loading)
        private set
    override var filters by mutableStateOf(ReplayFilters())
        private set

    override var scrollToTopIndex by mutableStateOf<Int?>(null)

    override val originalTeam: Teamlytics
        get() = (teamState as TeamState.Loaded).team

    override val filteredTeam: Teamlytics
        get() = _filteredTeam ?: originalTeam

    override val hasFilteredReplays get() = filters.hasAny()

    override var isApplyingFiltersLoading by mutableStateOf(false)
        private set

    private var _filteredTeam by mutableStateOf<Teamlytics?>(null)

    init {
        loadTeam()
    }

    private fun loadTeam() {
        scope.launch {
            val teamlyticsResult = useCase.get(teamId)
            withContext(Dispatchers.Main) {
                teamState = teamlyticsResult.fold(
                    ifLeft = { TeamState.Error(it) },
                    ifRight = { TeamState.Loaded(it) }
                )
            }
        }
    }

    override fun applyFilters(filters: ReplayFilters) {
        this.filters = filters
        isApplyingFiltersLoading = true
        scope.launch {
            reloadFilteredTeam()
            withContext(Dispatchers.Main) {
                isApplyingFiltersLoading = false
            }
        }
    }

    override suspend fun addReplays(replays: List<ReplayAnalytics>): Either<DomainError, Unit> {
        val currentTeam = originalTeam
        val teamReplays = currentTeam.replays.toMutableList()

        // add the new replays at the right spot, in case they were unordered
        for (newReplay in replays) {
            val previousIndex = teamReplays.indexOfFirst { it.nextBattleRef == newReplay.reference }
            if (previousIndex >= 0) {
                teamReplays.add(previousIndex + 1, newReplay)
                continue
            }
            val nextIndex = teamReplays.indexOfFirst { it.reference == newReplay.nextBattleRef }
            if (nextIndex >= 0) {
                teamReplays.add(nextIndex, newReplay)
                continue
            }
            teamReplays.add(newReplay)
        }

        return save(currentTeam.copy(replays = teamReplays.withComputedElo())).also { onReplaysModified() }
    }

    override suspend fun setMatchupNotes(matchupNotes: List<MatchupNotes>): Either<DomainError, Unit> {
        val currentTeam = originalTeam
        return save(currentTeam.copy(matchupNotes = matchupNotes)).also { onReplaysModified() }
    }

    override fun export(team: Teamlytics) = useCase.export(team)

    override suspend fun removeReplay(replay: ReplayAnalytics): Either<DomainError, Unit> {
        val currentTeam = originalTeam
        return save(currentTeam.copy(replays = currentTeam.replays - replay)).also { onReplaysModified() }
    }

    override suspend fun replaceReplay(original: ReplayAnalytics, replay: ReplayAnalytics): Either<DomainError, Unit> {
        val currentTeam = originalTeam
        val replayIndex = currentTeam.replays.indexOf(original)
        val replays = currentTeam.replays.mapIndexed { index, r ->
            if (index == replayIndex) replay else r
        }
        return save(currentTeam.copy(replays = replays.withComputedElo())).also { onReplaysModified() }
    }

    override suspend fun setNotes(team: Teamlytics, notes: TeamlyticsNotes?): Either<DomainError, Unit> {
        val currentTeam = this.originalTeam
        return save(currentTeam.copy(notes = notes))
    }

    override suspend fun setData(
        team: Teamlytics,
        data: TeamlyticsData
    ): Either<DomainError, Unit> {
        val currentTeam = this.originalTeam
        return save(currentTeam.copy(data = data))
    }

    private suspend fun save(team: Teamlytics): Either<DomainError, Unit> = either {
        val updatedTeam = useCase.save(team).bind()
        withContext(Dispatchers.Main) {
            teamState = TeamState.Loaded(updatedTeam)
        }
    }

    private fun onReplaysModified() {
        reloadFilteredTeam()
    }

    private fun reloadFilteredTeam() {
        _filteredTeam =
            if (hasFilteredReplays) originalTeam.copy(
                replays = originalTeam.withContext { originalTeam.replays.filter { filters.matches(it) } }
            )
            else originalTeam
    }
}