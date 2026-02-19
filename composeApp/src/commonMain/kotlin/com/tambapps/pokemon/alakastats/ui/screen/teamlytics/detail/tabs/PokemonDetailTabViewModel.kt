package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail.tabs

import com.tambapps.pokemon.alakastats.ui.composables.TabViewModel
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.util.VoidSignal

abstract class PokemonDetailTabViewModel: TabViewModel {

    override val scrollToTopSignal = VoidSignal()

    protected abstract val isTabLoading: Boolean

    abstract val pokemonImageService: PokemonImageService

}