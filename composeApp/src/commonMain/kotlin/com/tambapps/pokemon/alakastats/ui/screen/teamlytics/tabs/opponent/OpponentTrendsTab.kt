package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.opponent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.composables.PokemonStatCard
import com.tambapps.pokemon.alakastats.ui.composables.PokemonStatsRow
import com.tambapps.pokemon.alakastats.ui.composables.WheelPickerDialog
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.FilterBarButton
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.FiltersBar
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.Header
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.replay.NoReplay
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.theme.tabReplaysTextMarginTopMobile


@Composable
fun OpponentTrendsTab(viewModel: MatchupsViewModel) {
    LaunchedEffect(viewModel.useCase.filters) {
        viewModel.loadStats()
    }
    if (!viewModel.isLoading && viewModel.hasNoData) {
        NoReplay(viewModel)
        return
    }
    val isCompact = LocalIsCompact.current

    val scrollState = rememberScrollState()
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(scrollState)
            .padding(if (isCompact) PaddingValues(horizontal = 8.dp) else PaddingValues(top = 16.dp, bottom = 8.dp))
    ) {
        val space = if (isCompact) 42.dp else 64.dp
        if (isCompact) {
            Spacer(Modifier.height(tabReplaysTextMarginTopMobile))
        }
        FiltersBar(viewModel) {
            var showDialog by remember { mutableStateOf(false) }
            FilterBarButton(onClick = { showDialog = true }) {
                Text((if (isCompact) "Min. Attendance" else "Minimum Attendance") +
                        (if (viewModel.minimumAttendance > 1) ": ${viewModel.minimumAttendance}" else ""))
            }
            if (showDialog) {
                WheelPickerDialog(
                    title = "Minimum Attendance",
                    items = (1..10).toList(),
                    initialIndex = viewModel.minimumAttendance - 1,
                    onPicked = { viewModel.updateMinimumAttendance(it) },
                    onDismissRequest = { showDialog = false }
                )
            }
       }
        Spacer(Modifier.height(16.dp))
        Header(viewModel.useCase)
        BestMatchupsRow(viewModel)
        Spacer(Modifier.height(space))
        WorstMatchupsRow(viewModel)
        Spacer(Modifier.height(space))
        HighestAttendancesRow(viewModel)
        Spacer(Modifier.height(space))
        LowestAttendancesRow(viewModel)
        Spacer(Modifier.height(space))
        CommonLeadsStatsRow(viewModel)
        Spacer(Modifier.height(space))
        WorstLeadsStatsRow(viewModel)
        Spacer(Modifier.height(space))
    }
}

@Composable
fun BestMatchupsRow(viewModel: MatchupsViewModel) = PokemonStatsRow(
    viewModel = viewModel,
    title = "Best Matchups",
    stats = viewModel.bestMatchups,
    isDuo = false) { matchupStats ->
    PokemonStatCard(
        pokemonImageService = viewModel.pokemonImageService,
        title = "${matchupStats.rate.times(100).toInt()}%",
        text = winRatioText(winCount = matchupStats.winCount, total = matchupStats.attendanceCount),
        pokemonName = matchupStats.pokemonName,
        modifier = Modifier.size(256.dp).padding(bottom = 32.dp)
    )
}

@Composable
fun WorstMatchupsRow(viewModel: MatchupsViewModel) = PokemonStatsRow(
    viewModel = viewModel,
    title = "Worst Matchups",
    stats = viewModel.worstMatchups,
    isDuo = false) { matchupStats ->
    PokemonStatCard(
        pokemonImageService = viewModel.pokemonImageService,
        title = "${matchupStats.rate.times(100).toInt()}%",
        text = winRatioText(winCount = matchupStats.winCount, total = matchupStats.attendanceCount),
        pokemonName = matchupStats.pokemonName,
        modifier = Modifier.size(256.dp).padding(bottom = 32.dp)
    )
}

@Composable
fun HighestAttendancesRow(viewModel: MatchupsViewModel) = PokemonStatsRow(
    viewModel = viewModel,
    title = "Highest Attendance",
    stats = viewModel.highestAttendances,
    isDuo = false) { attendanceStats ->
    val attendanceCount = attendanceStats.attendanceCount
    val total = attendanceStats.totalGamesCount
    val text = when {
        attendanceCount == 0 && total > 1 -> "Not seen all $total games"
        attendanceCount == total && total == 1 -> "Seen\n1 out of 1\ngame"
        attendanceCount == total -> "Seen all\n$total games"
        else -> "Seen ${attendanceCount}\nout of ${total}\ngames"
    }
    PokemonStatCard(
        pokemonImageService = viewModel.pokemonImageService,
        title = "${attendanceStats.rate.times(100).toInt()}%",
        text = text,
        pokemonName = attendanceStats.pokemonName,
        modifier = Modifier.size(256.dp).padding(bottom = 32.dp)
    )
}

@Composable
fun LowestAttendancesRow(viewModel: MatchupsViewModel) = PokemonStatsRow(
    viewModel = viewModel,
    title = "Lowest Attendance",
    stats = viewModel.lowestAttendances,
    isDuo = false) { attendanceStats ->
    val attendanceCount = attendanceStats.attendanceCount
    val total = attendanceStats.totalGamesCount
    val text = when {
        attendanceCount == 0 -> "Not seen all $total games"
        attendanceCount == total && total == 1 -> "Seen\n1 out of 1\ngame"
        attendanceCount == total -> "Seen all\n$total games"
        else -> "Seen ${attendanceCount}\nout of ${total}\ngames"
    }
    PokemonStatCard(
        pokemonImageService = viewModel.pokemonImageService,
        title = "${attendanceStats.rate.times(100).toInt()}%",
        text = text,
        pokemonName = attendanceStats.pokemonName,
        modifier = Modifier.size(256.dp).padding(bottom = 32.dp)
    )
}

@Composable
private fun CommonLeadsStatsRow(viewModel: MatchupsViewModel) = PokemonStatsRow(
    viewModel = viewModel,
    title = if (LocalIsCompact.current) "Common Opp. Leads" else "Common Opposing Leads",
    stats = viewModel.commonLeads,
    isDuo = viewModel.commonLeads.firstOrNull()?.lead?.let { it.size >= 2 } == true,
    emptyMessage = if (!viewModel.filters.hasAny()) "Apply filters to see common opposing leads in a matchup" else "No data to display"
) { leadStats ->

    val attendanceCount = leadStats.attendanceCount
    val total = leadStats.totalGamesCount
    val text = when {
        attendanceCount == total && total == 1 -> "Won\n1 out of 1\ngame"
        attendanceCount == total -> "Lead all\n$total games"
        else -> "Led ${attendanceCount}\nout of $total\ngames"
    }
    PokemonStatCard(
        pokemonImageService = viewModel.pokemonImageService,
        title = "${leadStats.attendanceRate.times(100).toInt()}%",
        text = text,
        pokemonName = leadStats.lead.first(),
        pokemonName2 = leadStats.lead.getOrNull(1),
        modifier = Modifier.size(256.dp).padding(bottom = 32.dp)
    )

}

@Composable
private fun WorstLeadsStatsRow(viewModel: MatchupsViewModel) = PokemonStatsRow(
    viewModel = viewModel,
    title = if (LocalIsCompact.current) "Worst Opp. Leads" else "Worst Opposing Leads",
    stats = viewModel.worstLeads,
    isDuo = viewModel.worstLeads.firstOrNull()?.lead?.let { it.size >= 2 } == true,
    emptyMessage = if (!viewModel.filters.hasAny()) "Apply filters to see worst opposing leads in a matchup" else "No data to display"
) { leadStats ->
    PokemonStatCard(
        pokemonImageService = viewModel.pokemonImageService,
        title = "${leadStats.winRate.times(100).toInt()}%",
        text = winRatioText(winCount = leadStats.winCount, total = leadStats.attendanceCount, isDuo = true),
        pokemonName = leadStats.lead.first(),
        pokemonName2 = leadStats.lead.getOrNull(1),
        modifier = Modifier.size(256.dp).padding(bottom = 32.dp)
    )

}

private fun winRatioText(winCount: Int, total: Int, isDuo: Boolean = false) =  when {
    winCount == 0 && total > 1 -> if (isDuo) "Lost all\n$total games" else "Lost all $total games"
    winCount == total && total == 1 -> "Beat\n1 out of 1\ngame"
    winCount == total -> "Beat all\n$total games"
    else -> "Beat ${winCount}\nout of $total\ngames"
}