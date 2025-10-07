package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val progressBarHeight = 4.dp
@Composable
internal fun LinearProgressBarIfEnabled(enabled: Boolean, modifier: Modifier = Modifier) {
    if (enabled) {
        LinearProgressBar(modifier)
    } else {
        Spacer(modifier.height(progressBarHeight))
    }
}
@Composable
internal fun LinearProgressBar(modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        modifier = modifier
            .fillMaxWidth()
            .height(progressBarHeight)  // thickness
    )
}