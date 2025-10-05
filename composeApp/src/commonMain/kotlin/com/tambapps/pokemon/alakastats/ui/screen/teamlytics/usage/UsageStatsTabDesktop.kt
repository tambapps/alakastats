package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.usage

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun UsageStatsTabDesktop(viewModel: UsageStatsViewModel) {
    Row(
        Modifier.fillMaxSize()
            .padding(top = 32.dp, bottom = 8.dp)
    ) {
        Spacer(Modifier.width(128.dp))
        val cardWeight = 2f
        UsageCard(viewModel, modifier = Modifier.weight(cardWeight))
        val separatorSpacerModifier = Modifier.width(64.dp)
        Spacer(separatorSpacerModifier)
        UsageAndWinCard(viewModel, Modifier.weight(cardWeight))
        Spacer(separatorSpacerModifier)
        TeraAndWinCard(viewModel, Modifier.weight(cardWeight))
        Spacer(Modifier.width(128.dp))
    }
}
