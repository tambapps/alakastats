package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton

@Composable
internal fun DamageCalcScreenDesktop(viewModel: DamageCalcViewModel) {
    val navigator = LocalNavigator.currentOrThrow
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(all = 16.dp)
            .fillMaxSize(),
    ) {
        Box(Modifier.fillMaxWidth()) {
            BackIconButton(navigator, Modifier.align(Alignment.CenterStart))
            Text(
                "Damage Calc",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
                    .clickable { viewModel.scrollToTopSignal.emit() }
            )
        }
        DamageCalc(viewModel, Modifier.weight(1f))
    }
}
