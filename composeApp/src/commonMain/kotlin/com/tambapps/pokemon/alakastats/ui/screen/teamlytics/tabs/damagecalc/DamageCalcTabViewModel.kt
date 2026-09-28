package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.damagecalc

import com.tambapps.pokemon.alakastats.domain.usecase.ConsultTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.ui.screen.damagecalc.DamageCalcViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.TeamlyticsTabViewModel

class DamageCalcTabViewModel(
    override val useCase: ConsultTeamlyticsUseCase,
    val damageCalcViewModel: DamageCalcViewModel,
) : TeamlyticsTabViewModel() {
    override val isTabLoading = false

    val team get() = useCase.originalTeam
}
