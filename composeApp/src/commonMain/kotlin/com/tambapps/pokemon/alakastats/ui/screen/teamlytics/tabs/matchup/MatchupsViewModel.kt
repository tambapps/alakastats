package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.matchup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.GameOutput
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

    private val scope = CoroutineScope(Dispatchers.Default)


    fun loadStats() {
        if (isTabLoading) {
            return
        }
        isTabLoading = true
        scope.launch {
            val result = computeMatchupStats()
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                bestMatchups = result.first
                worstMatchups = result.second
                isTabLoading = false
            }
        }
    }

    private fun computeMatchupStats(): Pair<List<MatchupStats>, List<MatchupStats>> = useCase.filteredTeam.withContext {
        val replays = team.replays.filter { it.gameOutput != GameOutput.UNKNOWN }

        val sortedMatchups = buildMap {
            for (replay in replays) {
                val won = replay.gameOutput == GameOutput.WIN
                for (pokemonName in replay.opponentPlayer.selection) {
                    val currentStats = getOrPut(pokemonName.baseNormalized) { MatchupStats(pokemonName, 0, 0) }
                    this[pokemonName.baseNormalized] = currentStats.incr(won)
                }
            }
        }.values.sortedWith(compareBy({ - it.winRate }, { - it.attendanceCount }))

        sortedMatchups.take(MATCHUP_LIST_MAX_LENGTH).filter { it.winRate >= 0.5f } to
                sortedMatchups.takeLast(MATCHUP_LIST_MAX_LENGTH).reversed().filter { it.winRate < 0.5f }
    }
}

data class MatchupStats(
    val pokemonName: PokemonName,
    val winCount: Int,
    val attendanceCount: Int,
) {
    val winRate = winCount.toFloat() / attendanceCount
}

private fun MatchupStats.incr(hasWon: Boolean) = copy(
    winCount = if (hasWon) winCount + 1 else winCount,
    attendanceCount = attendanceCount + 1
)