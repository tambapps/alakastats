package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.damagecalc

import androidx.compose.runtime.Composable
import com.tambapps.pokemon.alakastats.ui.screen.damagecalc.DamageCalc

@Composable
fun DamageCalcTab(viewModel: DamageCalcTabViewModel) {
    // like the other tabs, clicking on the already selected tab scrolls back to the top
    viewModel.scrollToTopSignal.Listen {
        viewModel.damageCalcViewModel.scrollToTopSignal.emit()
    }
    // the tab is already in the teamlytics pager, swiping would conflict
    DamageCalc(viewModel.damageCalcViewModel, team = viewModel.team)
}
