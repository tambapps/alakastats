package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.usecase.ConsultTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.NbReplaysText
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.WinRateText

@Composable
internal fun Header(useCase: ConsultTeamlyticsUseCase) {
    if (useCase.originalTeam.replays.isEmpty()) {
        NbReplaysText(
            team = useCase.originalTeam,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        return
    }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.weight(1f))
        NbReplaysText(useCase)
        Spacer(Modifier.width(32.dp))
        WinRateText(useCase)
        Spacer(Modifier.weight(1f))
    }
    Spacer(Modifier.height(32.dp))
}
