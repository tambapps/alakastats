package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.move

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.GameOutput
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsContext
import com.tambapps.pokemon.alakastats.domain.model.withContext
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class PokemonUsages(
    val movesCount: Map<String, Int> = emptyMap(),
    val usageCount: Int,
    val winCount: Int,
    val teraCount: Int,
    val teraAndWinCount: Int
) {

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

class MoveUsageViewModel(
    val team: Teamlytics,
    val pokemonImageService: PokemonImageService,
) {
    var isLoading by mutableStateOf(false)
        private set

    val replays get() = team.replays

    private val scope = CoroutineScope(Dispatchers.Default)
    val pokemonPokemonUsages = mutableStateMapOf<PokemonName, PokemonUsages>()

    fun loadStats() {
        if (isLoading) {
            return
        }
        isLoading = true
        scope.launch {
            team.withContext {
                val replays = team.replays.filter { it.gameOutput != GameOutput.UNKNOWN }
                doLoadStats(replays)
            }
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                isLoading = false
            }
        }
    }

    private fun TeamlyticsContext.doLoadStats(replays: List<ReplayAnalytics>) {
        val result = replays.asSequence().map { replay ->
            fromReplay(replay)
        }
            .reduceOrNull { m1, m2 -> merged(m1, m2) } ?: emptyMap()
        pokemonPokemonUsages.clear()
        pokemonPokemonUsages.putAll(result)
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
        .associateWith { pokemonName ->
            val hasWon = replay.hasWon(player)
            val hasTerastallized = player.hasTerastallized(pokemonName)
            PokemonUsages(
                movesCount = player.movesUsage[pokemonName] ?: emptyMap(),
                usageCount = 1,
                winCount = if (hasWon) 1 else 0,
                teraCount = if (hasTerastallized) 1 else 0,
                teraAndWinCount = if (hasWon && hasTerastallized) 1 else 0
            )
        }
}