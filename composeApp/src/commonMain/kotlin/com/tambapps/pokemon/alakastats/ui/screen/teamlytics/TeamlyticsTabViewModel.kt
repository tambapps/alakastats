package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import com.tambapps.pokemon.alakastats.domain.usecase.ConsultTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageReplayFiltersUseCase
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.util.VoidSignal

abstract class TeamlyticsTabViewModel() {

    abstract val useCase: ConsultTeamlyticsUseCase

    val isLoading get() = isTabLoading || useCase.isApplyingFiltersLoading

    val scrollToTopSignal = VoidSignal()

    protected abstract val isTabLoading: Boolean

    abstract val pokemonImageService: PokemonImageService

}

abstract class TeamlyticsFiltersTabViewModel: TeamlyticsTabViewModel() {
    abstract override val useCase: ManageReplayFiltersUseCase

    val filters get() = useCase.filters
}