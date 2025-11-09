package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.tune
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.usecase.ConsultTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageReplayFiltersUseCase
import org.jetbrains.compose.resources.painterResource

private val textStyle @Composable get() = MaterialTheme.typography.titleLarge

@Composable
internal fun FiltersButton(useCase: ManageReplayFiltersUseCase, modifier: Modifier = Modifier) {
    BadgedBox(
        modifier = modifier,
        badge = {
            if (useCase.filters.hasAny()) {
                Badge(Modifier.size(16.dp))
            }
        }
    ) {
        FloatingActionButton(
            onClick = { useCase.openFilters() },
        ) {
            Icon(
                painter = painterResource(Res.drawable.tune),
                contentDescription = "Filters",
            )
        }
    }
}


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
fun WinRateText(teamlytics: Teamlytics, modifier: Modifier = Modifier) = WinRateText(remember { teamlytics.winRate }, modifier)

@Composable
fun WinRateText(winRate: Int, modifier: Modifier = Modifier) {
    Text("$winRate% winrate", style = textStyle, modifier = modifier)
}