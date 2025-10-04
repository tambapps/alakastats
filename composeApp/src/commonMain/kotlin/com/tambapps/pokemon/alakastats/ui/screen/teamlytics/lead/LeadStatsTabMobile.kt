package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.lead

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.composables.LinearProgressBar

@Composable
internal fun LeadStatsTabMobile(viewModel: LeadStatsViewModel) {
    Column(
        Modifier.fillMaxSize()
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp)
                .weight(1f)
        ) {
            if (viewModel.isLoading) {
                return@Column
            }
            Spacer(Modifier.height(128.dp))
            MostCommonLeadCard(viewModel)
            val spacerModifier = Modifier.height(42.dp)
            Spacer(spacerModifier)
            MostEffectiveLeadCard(viewModel)
            Spacer(spacerModifier)
            LeadAndWin(viewModel)
        }
        if (viewModel.isLoading) {
            LinearProgressBar(Modifier.fillMaxWidth())
        }
    }
}
