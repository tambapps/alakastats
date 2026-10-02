package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
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
internal fun DamageCalcScreenMobile(viewModel: DamageCalcViewModel) {
    val navigator = LocalNavigator.currentOrThrow
    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        // a compact top bar, to leave room for the calc
        Box(Modifier.fillMaxWidth()) {
            BackIconButton(navigator, Modifier.align(Alignment.CenterStart).size(40.dp))
            Text(
                "Damage Calc",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
                    .clickable { viewModel.scrollToTopSignal.emit() }
            )
        }
        Spacer(Modifier.height(4.dp))
        DamageCalc(viewModel, Modifier.weight(1f))
    }
}
