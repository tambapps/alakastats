package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
        when(val replayCount = replays.size) {
            1 -> "$replayCount replay"
            else -> "$replayCount replays"
        },
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
            when(val replaysCount = useCase.filteredTeam.replays.size) {
                0 -> "no replays\nmatched"
                1 -> "$replaysCount replay\nmatched"
                else -> "$replaysCount replays\nmatched"
            },
            modifier = modifier,
            textAlign = textAlign,
            style = textStyle
        )
    } else {
        NbReplaysText(useCase.filteredTeam, modifier = modifier, textAlign = textAlign)
    }
}

@Composable
fun WinRateText(useCase: ConsultTeamlyticsUseCase, modifier: Modifier = Modifier) = WinRateText(useCase.filteredTeam, modifier)

@Composable
fun WinRateText(teamlytics: Teamlytics, modifier: Modifier = Modifier) = WinRateText(teamlytics.winRate, modifier)

@Composable
fun WinRateText(winRate: Int, modifier: Modifier = Modifier) {
    Text("$winRate% winrate", style = textStyle, modifier = modifier)
}