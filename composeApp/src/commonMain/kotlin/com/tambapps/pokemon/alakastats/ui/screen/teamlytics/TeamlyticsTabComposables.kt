package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.usecase.ConsultTeamlyticsUseCase

private val textStyle @Composable get() = MaterialTheme.typography.titleLarge

@Composable
fun NbReplaysText(
    team: Teamlytics,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) = NbReplaysText(team.replays, modifier, textAlign)

@Composable
fun NbReplaysText(
    replays: List<ReplayAnalytics>,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    Text(
        "${replays.size} replays",
        modifier = modifier,
        textAlign = textAlign,
        style = textStyle
    )
}

@Composable
fun NbReplaysText(
    useCase: ConsultTeamlyticsUseCase,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = TextAlign.Center,
) {
    if (useCase.hasFilteredReplays) {
        Text(
            "${useCase.team.replays.size} replays\nmatched",
            modifier = modifier,
            textAlign = textAlign,
            style = textStyle
        )
    } else {
        NbReplaysText(useCase.team)
    }
}

@Composable
fun WinRateText(useCase: ConsultTeamlyticsUseCase, modifier: Modifier = Modifier) = WinRateText(useCase.team, modifier)

@Composable
fun WinRateText(teamlytics: Teamlytics, modifier: Modifier = Modifier) = WinRateText(remember { teamlytics.winRate }, modifier)

@Composable
fun WinRateText(winRate: Int, modifier: Modifier = Modifier) {
    Text("$winRate% winrate", style = textStyle, modifier = modifier)
}