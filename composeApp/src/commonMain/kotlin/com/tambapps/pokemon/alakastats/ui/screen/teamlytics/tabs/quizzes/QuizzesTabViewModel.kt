package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.quizzes

import com.tambapps.pokemon.alakastats.domain.usecase.ConsultTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.TeamlyticsTabViewModel

class QuizzesTabViewModel(
    override val useCase: ConsultTeamlyticsUseCase,
) : TeamlyticsTabViewModel() {
    override val isTabLoading = false

    val team get() = useCase.originalTeam
}
