package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail.tabs.speedscale

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import arrow.core.flatMap
import com.tambapps.pokemon.Pokemon
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
    var pokemons by mutableStateOf(emptyList<PokemonData>())

    fun loadSpeedScale(onError: (DomainError) -> Unit) {
        if (isTabLoading) return
        val format = team.format.takeIf { it != Format.NONE } ?: return
        isTabLoading = true
        scope.launch {
            val either = formatRepository.get(format)
                .flatMap { pokeApi.bulkGet(it.popularPokemons) }
            withContext(Dispatchers.Main) {
                isTabLoading = false
                either.fold(
                    ifLeft = onError,
                    ifRight = { result ->
                        pokemons = result.sortedWith(compareBy({ - it.stats.speed }, { it.name.value }))
                    }
                )
            }
        }
    }
}