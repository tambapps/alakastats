package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail.tabs.overview

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.PokemonData
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail.tabs.PokemonDetailTabViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.usage.PokemonUsages
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService

class PokemonDetailOverviewModel(
    override val pokemonImageService: PokemonImageService,
    val team: Teamlytics,
    val pokemon: Pokemon,
    val megaPokemon: PokemonName?,
    val pokemonData: PokemonData?,
    val notes: String?,
    val usages: PokemonUsages?,
    megaSelectedState: MutableState<Boolean>
) : PokemonDetailTabViewModel() {
    override val isTabLoading = false
    var megaSelected by megaSelectedState

}