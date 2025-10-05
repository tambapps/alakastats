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

class MoveUsageViewModel(
    val team: Teamlytics,
    val pokemonImageService: PokemonImageService,
) {
    var isLoading by mutableStateOf(false)
        private set

    private val scope = CoroutineScope(Dispatchers.Default)
    val pokemonMovesUsage = mutableStateMapOf<PokemonName, Map<String, Int>>()

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
            replay.youPlayer.movesUsage
        }
            .reduceOrNull { m1, m2 -> merged(m1, m2) } ?: emptyMap()
        pokemonMovesUsage.clear()
        pokemonMovesUsage.putAll(result)
    }

    private fun merged(
        m1: Map<PokemonName, Map<String, Int>>,
        m2: Map<PokemonName, Map<String, Int>>
    ): Map<PokemonName, Map<String, Int>> {
        return (m1.keys + m2.keys).associateWith { pokemon ->
            val moves1 = m1[pokemon] ?: emptyMap()
            val moves2 = m2[pokemon] ?: emptyMap()
            (moves1.keys + moves2.keys).associateWith { move ->
                (moves1[move] ?: 0) + (moves2[move] ?: 0)
            }
        }
    }

}