package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.domain.error.GetPokemonDataError
import com.tambapps.pokemon.alakastats.domain.model.PokemonData
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.usecase.ConsultPokemonDetailUseCase
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

sealed class TeamPokemonStateState {
    data object Loading : TeamPokemonStateState()
    data class Loaded(
        val team: Teamlytics,
        val pokemon: Pokemon,
        val pokemonData: PokemonData?
    ) : TeamPokemonStateState()
    data class Error(val error: DomainError) : TeamPokemonStateState()
}

class PokemonDetailViewModel(
    private val teamId: Uuid,
    private val pokemonName: PokemonName,
    private val useCase: ConsultPokemonDetailUseCase,
    val pokemonImageService: PokemonImageService
): ScreenModel {

    var state by mutableStateOf<TeamPokemonStateState>(TeamPokemonStateState.Loading)
        private set
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        loadTeamPokemon()
    }

    private fun loadTeamPokemon() {
        scope.launch {
            val teamlyticsResult = useCase.get(teamId)
            withContext(Dispatchers.Main) {
                state = teamlyticsResult.fold(
                    ifLeft = { TeamPokemonStateState.Error(it) },
                    ifRight = { team ->
                        val pokemon = team.pokePaste.pokemons.find { it.name.matches(pokemonName) }
                        val data = team.data.pokemonData[pokemonName]
                        if (pokemon == null) TeamPokemonStateState.Error(GetPokemonDataError("Could not find pokemon on team"))
                        else TeamPokemonStateState.Loaded(team, pokemon, data)
                    }
                )
            }
        }
    }
}