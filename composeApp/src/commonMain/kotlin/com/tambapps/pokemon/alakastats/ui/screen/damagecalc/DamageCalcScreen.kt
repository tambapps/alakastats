package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import androidx.compose.runtime.Composable
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import org.koin.compose.viewmodel.koinViewModel

/**
 * @param showBackButton false for the damage calc alone, on the web app's damage-calc page
 */
@Composable
fun DamageCalcScreen(showBackButton: Boolean) {
    val viewModel = koinViewModel<DamageCalcViewModel>()
    val isCompact = LocalIsCompact.current

    if (isCompact) {
        DamageCalcScreenMobile(viewModel, showBackButton)
    } else {
        DamageCalcScreenDesktop(viewModel, showBackButton)
    }
}
