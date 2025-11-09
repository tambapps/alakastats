package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.lead

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.NbReplaysText

@Composable
internal fun LeadStatsTabDesktop(viewModel: LeadStatsViewModel) {
    Column(Modifier.fillMaxSize().padding(top = 16.dp, bottom = 8.dp)) {
        NbReplaysText(viewModel.useCase, modifier = Modifier.fillMaxWidth()) // fill maxWidth to center text
        Spacer(Modifier.height(32.dp))
        Row(Modifier.weight(1f)) {
            Spacer(Modifier.width(128.dp))
            val cardWeight = 2f
            MostCommonLeadCard(viewModel, modifier = Modifier.weight(cardWeight))
            val separatorSpacerModifier = Modifier.width(64.dp)
            Spacer(separatorSpacerModifier)
            MostEffectiveLeadCard(viewModel, Modifier.weight(cardWeight))
            Spacer(separatorSpacerModifier)
            LeadAndWin(viewModel, Modifier.weight(cardWeight))
            Spacer(Modifier.width(128.dp))
        }
    }
}
