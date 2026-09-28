package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics

@Composable
internal fun DamageCalcDesktop(
    viewModel: DamageCalcViewModel,
    modifier: Modifier = Modifier,
    team: Teamlytics? = null,
) {
    Box(modifier.fillMaxSize())
}
