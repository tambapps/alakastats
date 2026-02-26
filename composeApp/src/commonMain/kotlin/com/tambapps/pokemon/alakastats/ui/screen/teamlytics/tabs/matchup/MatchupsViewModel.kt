package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.matchup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.GameOutput
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsContext
import com.tambapps.pokemon.alakastats.domain.model.withContext
import com.tambapps.pokemon.alakastats.domain.usecase.ManageReplayFiltersUseCase
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.TeamlyticsFiltersTabViewModel
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val MATCHUP_LIST_MAX_LENGTH = 10

class MatchupsViewModel(
    override val useCase: ManageReplayFiltersUseCase,
    override val pokemonImageService: PokemonImageService,
): TeamlyticsFiltersTabViewModel() {

    override var isTabLoading by mutableStateOf(false)
        private set

    val hasNoData get() = bestMatchups.isEmpty() && worstMatchups.isEmpty()

    var bestMatchups by mutableStateOf(emptyList<MatchupStats>())
        private set
    var worstMatchups by mutableStateOf(emptyList<MatchupStats>())
        private set

    var highestAttendances by mutableStateOf(emptyList<AttendanceStats>())
        private set
    var lowestAttendances by mutableStateOf(emptyList<AttendanceStats>())
        private set
    var commonLeads by mutableStateOf(emptyList<LeadStats>())
        private set
    var worstLeads by mutableStateOf(emptyList<LeadStats>())
        private set

    private val scope = CoroutineScope(Dispatchers.Default)

    fun loadStats() {
        if (isTabLoading) {
            return
        }
        isTabLoading = true
        scope.launch {
            val matchupStatsResult = computeMatchupStats()
            val attendanceStatsResult = computeAttendanceStats()
            val commonLeadsResult = computeCommonLeadStats()
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                bestMatchups = matchupStatsResult.first
                worstMatchups = matchupStatsResult.second
                highestAttendances = attendanceStatsResult.first
                lowestAttendances = attendanceStatsResult.second
                commonLeads = commonLeadsResult.first
                worstLeads = commonLeadsResult.second
                isTabLoading = false
            }
        }
    }

    private fun computeCommonLeadStats(): Pair<List<LeadStats>, List<LeadStats>> = useCase.filteredTeam.withContext {
        if (!filters.hasAny()) return@withContext emptyList<LeadStats>() to emptyList()
        val replays = team.replays.filter { it.gameOutput != GameOutput.UNKNOWN }

        val leadsStats = buildMap {
            for (replay in replays) {
                val opponentPlayer = replay.opponentPlayer
                val lead = opponentPlayer.lead
                val currentStats = getOrPut(lead) { LeadStats(lead, replays.size) }
                this[lead] = currentStats.incr(hasWon = replay.gameOutput == GameOutput.WIN)
            }
        }.values
        leadsStats.sortedBy { - it.attendanceCount }.take(MATCHUP_LIST_MAX_LENGTH) to
                leadsStats.sortedWith(compareBy({ it.winRate }, { - it.attendanceCount })).take(MATCHUP_LIST_MAX_LENGTH)

    }

    private fun computeMatchupStats(): Pair<List<MatchupStats>, List<MatchupStats>> = computeStats(
        statGenerator = ::MatchupStats,
        replayPokemonsSupplier = { it.opponentPlayer.selection },
        statUpdater = { replay, stats -> stats.incr(replay.gameOutput == GameOutput.WIN) },
        comparator = compareBy({ - it.rate }, { - it.attendanceCount })
    )

    private fun computeAttendanceStats(): Pair<List<AttendanceStats>, List<AttendanceStats>> = computeStats(
        statGenerator = ::AttendanceStats,
        replayPokemonsSupplier = { it.opponentPlayer.teamPokemonNames },
        statUpdater = { replay, stats -> stats.incr(replay.opponentPlayer.hasSelected(stats.pokemonName)) },
        comparator = compareBy({ - it.rate }, { - it.totalGamesCount })
    )

    private inline fun <T: Ratable> computeStats(
        statGenerator: (PokemonName) -> T,
        replayPokemonsSupplier: TeamlyticsContext.(ReplayAnalytics) -> List<PokemonName>,
        statUpdater: TeamlyticsContext.(ReplayAnalytics, T) -> T,
        comparator: Comparator<in T>
    ): Pair<List<T>, List<T>> = useCase.filteredTeam.withContext {
        val replays = team.replays.filter { it.gameOutput != GameOutput.UNKNOWN }

        val sortedStats = buildMap {
            for (replay in replays) {
                for (pokemonName in replayPokemonsSupplier.invoke(this@withContext, replay)) {
                    val currentStats = getOrPut(pokemonName.baseNormalized) { statGenerator.invoke(pokemonName) }
                    this[pokemonName.baseNormalized] = statUpdater.invoke(this@withContext, replay, currentStats)
                }
            }
        }.values.sortedWith(comparator)

        sortedStats.take(MATCHUP_LIST_MAX_LENGTH).filter { it.rate >= 0.5f } to
                sortedStats.takeLast(MATCHUP_LIST_MAX_LENGTH).reversed().filter { it.rate < 0.5f }
    }

}

interface Ratable {
    val rate: Float
}

data class MatchupStats(
    val pokemonName: PokemonName,
    val winCount: Int = 0,
    val attendanceCount: Int = 0,
): Ratable {
    override val rate = winCount.toFloat() / attendanceCount
}

data class AttendanceStats(
    val pokemonName: PokemonName,
    val attendanceCount: Int = 0,
    val totalGamesCount: Int = 0,
): Ratable {
    override val rate = attendanceCount.toFloat() / totalGamesCount
}

data class LeadStats(
    val lead: List<PokemonName>,
    val totalGamesCount: Int,
    val attendanceCount: Int = 0,
    val winCount: Int = 0,
    ) {
    val attendanceRate = attendanceCount.toFloat() / totalGamesCount
    val winRate = winCount.toFloat() / attendanceCount
}

private fun MatchupStats.incr(hasWon: Boolean) = copy(
    winCount = if (hasWon) winCount + 1 else winCount,
    attendanceCount = attendanceCount + 1
)

private fun AttendanceStats.incr(hasAttended: Boolean) = copy(
    attendanceCount = if (hasAttended) attendanceCount + 1 else attendanceCount,
    totalGamesCount = totalGamesCount + 1
)

private fun LeadStats.incr(hasWon: Boolean) = copy(
    attendanceCount = attendanceCount + 1,
    winCount = if (hasWon) winCount + 1 else winCount,
)