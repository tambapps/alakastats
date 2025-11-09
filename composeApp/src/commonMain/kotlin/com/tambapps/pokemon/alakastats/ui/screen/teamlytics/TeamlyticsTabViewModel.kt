package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import com.tambapps.pokemon.alakastats.domain.usecase.ConsultTeamlyticsUseCase

abstract class TeamlyticsTabViewModel {

    abstract val useCase: ConsultTeamlyticsUseCase

    val isLoading get() = isTabLoading || useCase.isApplyingFiltersLoading

    protected abstract val isTabLoading: Boolean
}