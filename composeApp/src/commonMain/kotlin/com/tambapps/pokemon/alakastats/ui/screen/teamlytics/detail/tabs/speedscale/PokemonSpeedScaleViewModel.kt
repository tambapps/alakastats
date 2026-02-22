package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail.tabs.speedscale

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import arrow.core.flatMap
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.domain.model.PokemonData
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.repository.FormatDataRepository
import com.tambapps.pokemon.alakastats.infrastructure.repository.PokeApiPokemonDataRepository
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail.tabs.PokemonDetailTabViewModel
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PokemonSpeed(
    val pokemonName: PokemonName,
    val value: Int,
    val boostNature: Boolean,
    val ev: Int
)

class PokemonSpeedScaleViewModel(
    override val pokemonImageService: PokemonImageService,
    val team: Teamlytics,
    val pokemon: Pokemon,
    val pokemonData: PokemonData?,
    private val pokeApi: PokeApiPokemonDataRepository,
    private val formatRepository: FormatDataRepository
) : PokemonDetailTabViewModel() {
    override var isTabLoading by mutableStateOf(false)

    private val scope = CoroutineScope(Dispatchers.Main)
    private var pokemons by mutableStateOf(emptyList<PokemonData>())

    var speedScale by mutableStateOf(emptyList<PokemonSpeed>())
        private set

    fun loadSpeedScale(onError: (DomainError) -> Unit) {
        if (isTabLoading) return
        val format = team.format.takeIf { it != Format.NONE } ?: return
        isTabLoading = true
        scope.launch {
            formatRepository.get(format)
                .flatMap { pokeApi.bulkGet(it.popularPokemons) }.fold(
                    ifLeft = {
                        withContext(Dispatchers.Main) {
                            isTabLoading = false
                            onError.invoke(it)
                        }
                    },
                    ifRight = { resultPokemons ->
                        val sortedPokemons = resultPokemons.sortedWith(compareBy({ - it.stats.speed }, { it.name.value }))
                        val scale = computeScale(sortedPokemons)
                        withContext(Dispatchers.Main) {
                            isTabLoading = false
                            pokemons = sortedPokemons
                            speedScale = scale
                        }
                    }
                )
        }
    }

    private fun computeScale(pokemons: List<PokemonData>): List<PokemonSpeed> {
        return pokemons.map {
            PokemonSpeed(it.name, it.stats.speed, false, 0)
        }
    }
}