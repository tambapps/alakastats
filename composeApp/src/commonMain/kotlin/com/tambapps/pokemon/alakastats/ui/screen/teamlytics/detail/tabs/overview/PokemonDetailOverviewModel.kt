package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail.tabs.overview

import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.alakastats.domain.model.PokemonData
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail.tabs.PokemonDetailTabViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.usage.PokemonUsages
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService

class PokemonDetailOverviewModel(
    override val pokemonImageService: PokemonImageService,
    val team: Teamlytics,
    val pokemon: Pokemon,
    val pokemonData: PokemonData?,
    val notes: String?,
    val usages: PokemonUsages?,
) : PokemonDetailTabViewModel() {
    override val isTabLoading = false

}