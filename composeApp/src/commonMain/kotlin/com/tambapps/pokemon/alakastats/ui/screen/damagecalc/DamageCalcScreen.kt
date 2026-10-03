package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact

object DamageCalcScreen : Screen {
    @Composable
    override fun Content() = DamageCalcScreenContent(koinScreenModel(), showBackButton = true)
}

/**
 * The damage calc as the app's only screen (e.g. the web app's damage-calc page), so without a back button
 */
object StandaloneDamageCalcScreen : Screen {
    @Composable
    override fun Content() = DamageCalcScreenContent(koinScreenModel(), showBackButton = false)
}

@Composable
private fun DamageCalcScreenContent(viewModel: DamageCalcViewModel, showBackButton: Boolean) {
    val isCompact = LocalIsCompact.current

    if (isCompact) {
        DamageCalcScreenMobile(viewModel, showBackButton)
    } else {
        DamageCalcScreenDesktop(viewModel, showBackButton)
    }
}
