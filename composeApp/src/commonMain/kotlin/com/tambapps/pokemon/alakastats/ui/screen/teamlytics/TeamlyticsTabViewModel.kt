package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.alakastats.domain.usecase.ConsultTeamlyticsUseCase

abstract class TeamlyticsTabViewModel() {

    abstract val useCase: ConsultTeamlyticsUseCase

    val isLoading get() = isTabLoading || useCase.isApplyingFiltersLoading

    // Use Int counter instead of Boolean to ensure LaunchedEffect always triggers
    var scrollToTopSignal by mutableStateOf(0)
        private set

    protected abstract val isTabLoading: Boolean

    fun signalScrollToTop() {
        scrollToTopSignal++
    }
}