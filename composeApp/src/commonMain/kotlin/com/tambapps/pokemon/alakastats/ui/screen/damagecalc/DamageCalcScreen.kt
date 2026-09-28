package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact

object DamageCalcScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<DamageCalcViewModel>()
        val isCompact = LocalIsCompact.current

        if (isCompact) {
            DamageCalcScreenMobile(viewModel)
        } else {
            DamageCalcScreenDesktop(viewModel)
        }
    }
}
