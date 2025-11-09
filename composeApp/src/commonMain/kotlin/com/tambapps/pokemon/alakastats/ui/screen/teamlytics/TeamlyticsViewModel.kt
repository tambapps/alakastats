package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.navigator.Navigator
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsNotes
import com.tambapps.pokemon.alakastats.domain.model.withComputedElo
import com.tambapps.pokemon.alakastats.domain.usecase.HandleTeamOverviewUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.HandleTeamReplaysUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.TeamlyticsUseCase
import com.tambapps.pokemon.alakastats.infrastructure.service.ReplayAnalyticsService
import com.tambapps.pokemon.alakastats.ui.model.ReplayFilters
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

class TeamlyticsViewModel(
    private val useCase: TeamlyticsUseCase,
    private val replayService: ReplayAnalyticsService,
    val imageService: PokemonImageService,
) : ScreenModel, HandleTeamReplaysUseCase, HandleTeamOverviewUseCase {

    private val scope = CoroutineScope(Dispatchers.Default)
    val teamState = mutableStateOf<Teamlytics?>(null)
    override var filters by mutableStateOf(ReplayFilters())
        private set
    var showFiltersDialog by mutableStateOf(false)


    override fun applyFilters(filters: ReplayFilters) {
        this.filters = filters
        closeFilters()
    }
    override fun openFilters() {
        showFiltersDialog = true
    }

    override fun closeFilters() {
        showFiltersDialog = false
    }

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

    override suspend fun addReplays(replays: List<ReplayAnalytics>): Either<DomainError, Unit> {
        val team = requireTeam()
        val teamReplays = team.replays.toMutableList()

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

        return save(team.copy(replays = teamReplays.withComputedElo()))
    }

    override suspend fun setNotes(team: Teamlytics, notes: TeamlyticsNotes?): Either<DomainError, Unit> {
        val team = requireTeam()
        return save(team.copy(notes = notes))
    }

    override fun export(team: Teamlytics) = useCase.export(team)

    override suspend fun removeReplay(replay: ReplayAnalytics): Either<DomainError, Unit> {
        val team = requireTeam()
        return save(team.copy(replays = team.replays - replay))
    }

    override suspend fun replaceReplay(original: ReplayAnalytics, replay: ReplayAnalytics): Either<DomainError, Unit> {
        val team = requireTeam()
        val replayIndex = team.replays.indexOf(original)
        val replays = team.replays.mapIndexed { index, r ->
            if (index == replayIndex) replay else r
        }
        return save(team.copy(replays = replays))
    }

    private suspend fun save(team: Teamlytics): Either<DomainError, Unit> = either {
        val updatedTeam = useCase.save(team).bind()
        withContext(Dispatchers.Main) {
            teamState.value = updatedTeam
        }
    }
}