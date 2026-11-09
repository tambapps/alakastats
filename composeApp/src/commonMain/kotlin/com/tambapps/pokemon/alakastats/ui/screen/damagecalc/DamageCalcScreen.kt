package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import androidx.compose.runtime.Composable
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import org.koin.compose.viewmodel.koinViewModel

/**
 * No back button: it's one of the parts of the app, reached from the navigation bar, or the damage calc alone on the
 * web app's damage-calc page
 */
@Composable
fun DamageCalcScreen() {
    val viewModel = koinViewModel<DamageCalcViewModel>()
    val isCompact = LocalIsCompact.current

    if (isCompact) {
        DamageCalcScreenMobile(viewModel)
    } else {
        DamageCalcScreenDesktop(viewModel)
    }
}
