package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.usage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.GameOutput
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsContext
import com.tambapps.pokemon.alakastats.domain.model.Terastallization
import com.tambapps.pokemon.alakastats.domain.model.withContext
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UsageStatsViewModel(
    val team: Teamlytics,
    val pokemonImageService: PokemonImageService,
    ) {

    var isLoading by mutableStateOf(false)
        private set

    val pokemonUsageMap = mutableStateMapOf<PokemonName, UsageStat>()
    val pokemonUsageAndWinMap = mutableStateMapOf<PokemonName, UsageStat>()
    val teraAndWinMap = mutableStateMapOf<Terastallization, UsageStat>()

    private val scope = CoroutineScope(Dispatchers.Default)

    fun loadStats() {
        if (isLoading) {
            return
        }
        isLoading = true
        scope.launch {
            team.withContext {
                val replays = team.replays.filter { it.gameOutput != GameOutput.UNKNOWN }
                loadUsageStats(replays)
                loadUsageAndWinStats(replays)
                loadTeraAndWinStats(replays)
            }
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                isLoading = false
            }
        }
    }

    private fun TeamlyticsContext.loadUsageStats(replays: List<ReplayAnalytics>) {
        val result = team.pokePaste.pokemons.asSequence().map { it.name }
            .associateWith { pokemonName ->
                val usage = replays.filter { replay -> replay.youPlayer.hasSelected(pokemonName) }.size

                val total = replays.size
                UsageStat(
                    usage = usage,
                    totalGames = total,
                    usageRate = if (total != 0) usage.toFloat() / total.toFloat() else 0f
                )
            }
        pokemonUsageMap.clear()
        pokemonUsageMap.putAll(result)
    }

    private fun TeamlyticsContext.loadUsageAndWinStats(replays: List<ReplayAnalytics>) {
        val result = team.pokePaste.pokemons.asSequence().map { it.name }
            .associateWith { pokemonName ->
                val pokemonReplays = replays.filter { replay -> replay.youPlayer.hasSelected(pokemonName) }
                val usageAndWin = pokemonReplays.filter { replay -> replay.gameOutput == GameOutput.WIN }.size

                val total = pokemonReplays.size
                UsageStat(
                    usage = usageAndWin,
                    totalGames = total,
                    usageRate = if (total != 0) usageAndWin.toFloat() / total.toFloat() else 0f
                )
            }
        pokemonUsageAndWinMap.clear()
        pokemonUsageAndWinMap.putAll(result)
    }

    private fun TeamlyticsContext.loadTeraAndWinStats(replays: List<ReplayAnalytics>) {
        val teraAndWinPerTera: Map<PokemonName, Map<PokeType, List<ReplayAnalytics>>> = team.pokePaste.pokemons.asSequence().map { it.name }
            .associateWith { pokemonName ->
                val pokemonTeraReplays = replays.filter { replay ->
                            replay.youPlayer.hasSelected(pokemonName)
                            && replay.youPlayer.hasTerastallized(pokemonName)
                }
                pokemonTeraReplays.groupBy { it.youPlayer.terastallization!!.type }
            }

        val result: Map<Terastallization, UsageStat> = teraAndWinPerTera.flatMap { (pokemon, replaysPerTera) ->
            replaysPerTera.map { (teraType, teraReplaysReplays) ->
                val teraAndWin = teraReplaysReplays.filter { it.gameOutput == GameOutput.WIN }.size
                val total = replays.size

                Terastallization(pokemon, teraType) to UsageStat(
                    usage = teraAndWin,
                    totalGames = total,
                    usageRate = if (total != 0) teraAndWin.toFloat() / total.toFloat() else 0f,
                )
            }
        }.toMap()

        // add tera specified from the pokepaste if missing
        val missingPokemonTera = team.pokePaste.pokemons
            .asSequence()
            .filter { it.teraType != null }
            .map { Terastallization(it.name, it.teraType!!) }
            .filter { pokeTera -> !result.keys.any { it -> pokeTera.pokemon.matches(it.pokemon) && pokeTera.type == it.type } }
            .toList()

        val completedResult = result + missingPokemonTera.associateWith { UsageStat(0, 0, 0f) }

        teraAndWinMap.clear()
        teraAndWinMap.putAll(completedResult)
    }
}


data class UsageStat(
    val usage: Int,
    val totalGames: Int,
    val usageRate: Float,
)