package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.alakastats.domain.usecase.ConsultTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.ui.util.VoidSignal

abstract class TeamlyticsTabViewModel() {

    abstract val useCase: ConsultTeamlyticsUseCase

    val isLoading get() = isTabLoading || useCase.isApplyingFiltersLoading

    // Use Int counter instead of Boolean to ensure LaunchedEffect always triggers
    var scrollToTopSignal = VoidSignal()

    protected abstract val isTabLoading: Boolean

}