package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import arrow.core.Either
import arrow.core.raise.either
import cafe.adriel.voyager.core.model.ScreenModel
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.domain.model.Player
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsNotes
import com.tambapps.pokemon.alakastats.domain.model.withComputedElo
import com.tambapps.pokemon.alakastats.domain.model.withContext
import com.tambapps.pokemon.alakastats.domain.usecase.ConsultTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamOverviewUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamReplaysUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.infrastructure.service.ReplayAnalyticsService
import com.tambapps.pokemon.alakastats.ui.model.PokemonFilter
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
    private val replayService: ReplayAnalyticsService,
    val imageService: PokemonImageService,
) : ScreenModel, ConsultTeamlyticsUseCase, ManageTeamReplaysUseCase, ManageTeamOverviewUseCase {

    private val scope = CoroutineScope(Dispatchers.Default)
    var teamState by mutableStateOf<TeamState>(TeamState.Loading)
        private set
    override var filters by mutableStateOf(ReplayFilters())
        private set
    var showFiltersDialog by mutableStateOf(false)

    override val team: Teamlytics
        get() = (teamState as TeamState.Loaded).team

    override val hasFiltered get() = filters.hasAny()

    // TODO use me
    var isLoading by mutableStateOf(false)
        private set

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
        closeFilters()
    }
    override fun openFilters() {
        showFiltersDialog = true
    }

    override fun closeFilters() {
        showFiltersDialog = false
    }

    override suspend fun parseReplay(url: String) = replayService.fetch(url)

    override suspend fun addReplays(replays: List<ReplayAnalytics>): Either<DomainError, Unit> {
        val currentTeam = team
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

    override fun export(team: Teamlytics) = useCase.export(team)

    override suspend fun removeReplay(replay: ReplayAnalytics): Either<DomainError, Unit> {
        val currentTeam = team
        return save(currentTeam.copy(replays = currentTeam.replays - replay)).also { onReplaysModified() }
    }

    override suspend fun replaceReplay(original: ReplayAnalytics, replay: ReplayAnalytics): Either<DomainError, Unit> {
        val currentTeam = team
        val replayIndex = currentTeam.replays.indexOf(original)
        val replays = currentTeam.replays.mapIndexed { index, r ->
            if (index == replayIndex) replay else r
        }
        return save(currentTeam.copy(replays = replays)).also { onReplaysModified() }
    }

    override suspend fun setNotes(team: Teamlytics, notes: TeamlyticsNotes?): Either<DomainError, Unit> {
        val currentTeam = this.team
        return save(currentTeam.copy(notes = notes))
    }

    private suspend fun save(team: Teamlytics): Either<DomainError, Unit> = either {
        val updatedTeam = useCase.save(team).bind()
        withContext(Dispatchers.Main) {
            teamState = TeamState.Loaded(updatedTeam)
        }
    }

    private suspend fun onReplaysModified() {
        withContext(Dispatchers.Main) {
            isLoading = true
        }
        /*
        TODO update teamlytics
        replays = if (hasFiltered) allReplays.filter { filters.matches(it) }
        else allReplays
         */

        withContext(Dispatchers.Main) {
            isLoading = false
        }
    }

    private fun ReplayFilters.matches(replay: ReplayAnalytics) = team.withContext {
        when {
            opponentTeam.isNotEmpty() && !teamMatches(replay.opponentPlayer, opponentTeam) -> false
            opponentSelection.isNotEmpty() && !selectionMatches(replay.opponentPlayer, opponentSelection) -> false
            yourSelection.isNotEmpty() && !selectionMatches(replay.youPlayer, yourSelection) -> false
            else -> true
        }
    }
}

private fun teamMatches(player: Player, pokemonFilters: List<PokemonFilter>): Boolean {
    for (pokemonFilter in pokemonFilters) {
        if (player.teamPreview.pokemons.none { it.name.matches(pokemonFilter.name) }) {
            return false
        }
        if (pokemonFilter.asLead && player.lead.none { it.matches(pokemonFilter.name) }) {
            return false
        }
    }
    return true
}

private fun selectionMatches(player: Player, pokemonFilters: List<PokemonFilter>): Boolean {
    for (pokemonFilter in pokemonFilters) {
        if (player.selection.none { it.matches(pokemonFilter.name) }) {
            return false
        }
        if (pokemonFilter.asLead && player.lead.none { it.matches(pokemonFilter.name) }) {
            return false
        }
    }
    return true
}