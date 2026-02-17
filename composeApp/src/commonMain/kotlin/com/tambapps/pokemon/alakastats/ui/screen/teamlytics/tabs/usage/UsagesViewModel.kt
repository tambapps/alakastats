package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.usage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.GameOutput
import com.tambapps.pokemon.alakastats.domain.model.Player
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsContext
import com.tambapps.pokemon.alakastats.domain.model.withContext
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamReplaysUseCase
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.TeamlyticsFiltersTabViewModel
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UsagesViewModel(
    override val useCase: ManageTeamReplaysUseCase,
    override val pokemonImageService: PokemonImageService,
): TeamlyticsFiltersTabViewModel() {

    override var isTabLoading by mutableStateOf(false)
        private set

    val team get() = useCase.filteredTeam
    val replays get() = useCase.filteredTeam.replays

    private val scope = CoroutineScope(Dispatchers.Default)
    val pokemonPokemonUsages = mutableStateMapOf<PokemonName, PokemonUsages>()

    fun loadStats() {
        if (isTabLoading) {
            return
        }
        isTabLoading = true
        pokemonPokemonUsages.clear()
        scope.launch {
            val result = useCase.filteredTeam.withContext {
                val replays = team.replays.filter { it.gameOutput != GameOutput.UNKNOWN }
                computeStats(replays)
            }
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                pokemonPokemonUsages.putAll(result)
                isTabLoading = false
            }
        }
    }

    private fun TeamlyticsContext.computeStats(replays: List<ReplayAnalytics>): Map<PokemonName, PokemonUsages> {
        return replays.asSequence().map { replay ->
            fromReplay(replay)
        }
            .reduceOrNull { m1, m2 -> merged(m1, m2) } ?: emptyMap()
    }

    private fun merged(
        m1: Map<PokemonName, PokemonUsages>,
        m2: Map<PokemonName, PokemonUsages>
    ): Map<PokemonName, PokemonUsages> {
        return (m1.keys + m2.keys).associateWith { pokemon ->
            val usage1 = m1[pokemon]
            val usage2 = m2[pokemon]
            when {
                usage1 != null && usage2 != null -> usage1.mergeWith(usage2)
                usage1 != null -> usage1
                usage2 != null -> usage2
                else -> throw RuntimeException("Shouldn't happen")
            }
        }
    }
}

private fun TeamlyticsContext.fromReplay(replay: ReplayAnalytics): Map<PokemonName, PokemonUsages> {
    val player = replay.youPlayer
    return player.selection.asSequence()
        .associateWith { pokemonName -> PokemonUsages.from(replay, replay.youPlayer, pokemonName)
        }
}

data class PokemonUsages(
    val movesCount: Map<String, Int> = emptyMap(),
    val usageCount: Int,
    val winCount: Int,
    val teraCount: Int,
    val teraAndWinCount: Int
) {

    companion object {

        fun from(
            replay: ReplayAnalytics,
            player: Player,
            pokemonName: PokemonName,
        ): PokemonUsages {
            val hasWon = replay.hasWon(player)
            val hasTerastallized = player.hasTerastallized(pokemonName)

            return PokemonUsages(
                movesCount = player.movesUsage[pokemonName] ?: emptyMap(),
                usageCount = 1,
                winCount = if (replay.hasWon(player)) 1 else 0,
                teraCount = if (hasTerastallized) 1 else 0,
                teraAndWinCount = if (hasWon && hasTerastallized) 1 else 0
            )
        }
    }
    fun mergeWith(pokemonUsages: PokemonUsages) = PokemonUsages(
        movesCount = (this.movesCount.keys + pokemonUsages.movesCount.keys).associateWith { move ->
            (this.movesCount[move] ?: 0) + (pokemonUsages.movesCount[move] ?: 0)
        },
        usageCount = this.usageCount + pokemonUsages.usageCount,
        winCount = this.winCount + pokemonUsages.winCount,
        teraCount = this.teraCount + pokemonUsages.teraCount,
        teraAndWinCount = this.teraAndWinCount + pokemonUsages.teraAndWinCount
    )
}
