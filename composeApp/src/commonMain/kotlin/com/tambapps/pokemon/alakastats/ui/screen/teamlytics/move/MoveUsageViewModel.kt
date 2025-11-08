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

data class MovesUsage(
    val movesCount: Map<String, Int> = emptyMap(),
    val replaysCount: Int
) {

    fun mergeWith(movesUsage: MovesUsage) = MovesUsage(
        movesCount = (movesCount.keys + movesUsage.movesCount.keys).associateWith { move ->
            (movesCount[move] ?: 0) + (movesUsage.movesCount[move] ?: 0)
        },
        replaysCount = this.replaysCount + movesUsage.replaysCount
    )
}

class MoveUsageViewModel(
    val team: Teamlytics,
    val pokemonImageService: PokemonImageService,
) {
    var isLoading by mutableStateOf(false)
        private set

    private val scope = CoroutineScope(Dispatchers.Default)
    val pokemonMovesUsage = mutableStateMapOf<PokemonName, MovesUsage>()

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
        pokemonMovesUsage.clear()
        pokemonMovesUsage.putAll(result)
    }

    private fun merged(
        m1: Map<PokemonName, MovesUsage>,
        m2: Map<PokemonName, MovesUsage>
    ): Map<PokemonName, MovesUsage> {
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

private fun TeamlyticsContext.fromReplay(replay: ReplayAnalytics): Map<PokemonName, MovesUsage> {
    val player = replay.youPlayer
    return player.selection.asSequence()
        .associateWith { pokemonName ->
            MovesUsage(
                movesCount = player.movesUsage[pokemonName] ?: emptyMap(),
                replaysCount = 1
            )
        }
}