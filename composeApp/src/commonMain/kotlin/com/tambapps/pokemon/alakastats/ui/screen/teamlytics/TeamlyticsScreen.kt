package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.domain.usecase.HandleTeamOverviewUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.HandleTeamReplaysUseCase
import com.tambapps.pokemon.alakastats.ui.composables.ExpansionTile
import com.tambapps.pokemon.alakastats.ui.model.ReplayFilters
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.lead.LeadStatsTab
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.lead.LeadStatsViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.move.MoveUsageTab
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.move.MoveUsageViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.overview.OverviewTab
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.overview.OverviewViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.replay.TeamReplayTab
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.replay.TeamReplayViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.usage.UsageStatsTab
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.usage.UsageStatsViewModel
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import kotlin.uuid.Uuid

data class TeamlyticsScreen(val teamId: Uuid) : Screen {
    private companion object {
        val TABS = listOf("Overview", "Replays", "Move Usages", "Lead Stats", "Usage Stats", "Match-up Notes")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<TeamlyticsViewModel>()
        val navigator = LocalNavigator.currentOrThrow
        LaunchedEffect(Unit) {
            viewModel.initTeam(teamId, navigator)
        }
        val isCompact = LocalIsCompact.current

        val pagerState = rememberPagerState(pageCount = { TABS.size })

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .safeContentPadding()
        ) {
            if (viewModel.team == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else if (isCompact) {
                TeamlyticsScreenMobile(viewModel, TABS, pagerState)
            } else {
                TeamlyticsScreenDesktop(viewModel, TABS, pagerState)
            }
        }
        if (viewModel.showFiltersDialog) {
            FiltersDialog(viewModel, viewModel.filters)
        }
    }
}

@Composable
private fun FiltersDialog(viewModel: TeamlyticsViewModel, filters: ReplayFilters) {
    Dialog(onDismissRequest = { viewModel.closeFilters() }) {
        Card(Modifier.fillMaxSize()) {
            Column(Modifier.padding(vertical = 16.dp, horizontal = 8.dp)) {
                Column(Modifier.weight(1f)
                    .verticalScroll(rememberScrollState()),
                    ) {
                    ExpansionTile(
                        title = {
                            Text(
                                text = "Opponent's team",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        },
                        content = {
                            Column {

                                Text("TODO")
                            }
                        }
                    )
                }
                Row(Modifier.padding(horizontal = 8.dp)) {
                    TextButton(onClick = { viewModel.closeFilters() }) {
                        Text("Close")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { viewModel.closeFilters() }) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { viewModel.closeFilters() }) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@Composable
internal fun Pager(
    modifier: Modifier,
    viewModel: TeamlyticsViewModel,
    pagerState: PagerState
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier
    ) { page ->
        val team = viewModel.requireTeam()
        when (page) {
            0 -> {
                val viewModel = koinInjectUseCase<HandleTeamOverviewUseCase, OverviewViewModel>(viewModel)
                OverviewTab(viewModel)
            }
            1 -> {
                val viewModel = koinInjectUseCase<HandleTeamReplaysUseCase, TeamReplayViewModel>(viewModel)
                TeamReplayTab(viewModel)
            }
            2 -> {
                val viewModel = koinInject<MoveUsageViewModel> {
                    parametersOf(team)
                }
                MoveUsageTab(viewModel)
            }
            3 -> {
                val viewModel = koinInject<LeadStatsViewModel> {
                    parametersOf(team)
                }
                LeadStatsTab(viewModel)
            }
            4 -> {
                val viewModel = koinInject<UsageStatsViewModel> {
                    parametersOf(team)
                }
                UsageStatsTab(viewModel)
            }
            5 -> {
                Box(Modifier.fillMaxSize()) {
                    Text("Coming soon", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
private inline fun <reified USE_CASE, reified T> koinInjectUseCase(viewModel: TeamlyticsViewModel) = koinInject<T> {
    parametersOf(viewModel as USE_CASE, viewModel.team)
}

@Composable
internal fun TabRowContent(
    pagerState: PagerState,
    tabs: List<String>
) {
    val scope = rememberCoroutineScope()
    tabs.forEachIndexed { index, title ->
        Tab(
            selected = pagerState.currentPage == index,
            onClick = {
                scope.launch {
                    pagerState.animateScrollToPage(index)
                }
            },
            text = { Text(title) }
        )
    }
}