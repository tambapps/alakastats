package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.damagecalc

import androidx.compose.runtime.Composable
import com.tambapps.pokemon.alakastats.ui.screen.damagecalc.DamageCalc

@Composable
fun DamageCalcTab(viewModel: DamageCalcTabViewModel) {
    // the tab is already in the teamlytics pager, swiping would conflict
    DamageCalc(viewModel.damageCalcViewModel, pagerSwipeEnabled = false)
}
