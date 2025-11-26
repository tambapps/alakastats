package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.domain.usecase.ConsultTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamOverviewUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamReplaysUseCase
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.lead.LeadStatsTab
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.lead.LeadStatsViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.matchup.MatchupNotesTab
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.matchup.MatchupNotesViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.usage.UsagesTab
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.usage.UsagesViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.overview.OverviewTab
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.overview.OverviewViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.replay.TeamReplayTab
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.replay.TeamReplayViewModel
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import kotlin.uuid.Uuid

data class TeamlyticsScreen(val teamId: Uuid) : Screen {
    private companion object {
        val TABS = listOf("Overview", "Replays", "Usages", "Lead Stats", "Matchup Notes")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<TeamlyticsViewModel> { parametersOf(teamId) }
        val navigator = LocalNavigator.currentOrThrow
        val isCompact = LocalIsCompact.current

        // Handle error state - navigate back if team loading failed
        LaunchedEffect(viewModel.teamState) {
            if (viewModel.teamState is TeamState.Error) {
                navigator.pop()
            }
        }

        val pagerState = rememberPagerState(pageCount = { TABS.size })

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .safeContentPadding()
        ) {
            when (viewModel.teamState) {
                is TeamState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                is TeamState.Error -> {
                    // Error is handled by LaunchedEffect above (navigator.pop())
                    // This branch is just to satisfy the when expression
                }
                is TeamState.Loaded -> {
                    if (isCompact) {
                        TeamlyticsScreenMobile(viewModel, TABS, pagerState)
                    } else {
                        TeamlyticsScreenDesktop(viewModel, TABS, pagerState)
                    }
                    if (viewModel.showFiltersDialog) {
                        val filtersViewModel = remember(viewModel.filters) { FiltersViewModel(viewModel, viewModel.imageService) }
                        FiltersDialog(filtersViewModel)
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
        when (page) {
            0 -> {
                val viewModel = koinInjectUseCase<ManageTeamOverviewUseCase, OverviewViewModel>(viewModel)
                OverviewTab(viewModel)
            }
            1 -> {
                val viewModel = koinInjectUseCase<ManageTeamReplaysUseCase, TeamReplayViewModel>(viewModel)
                TeamReplayTab(viewModel)
            }
            2 -> {
                val viewModel = koinInjectUseCase<ConsultTeamlyticsUseCase, UsagesViewModel>(viewModel)
                UsagesTab(viewModel)
            }
            3 -> {
                val viewModel = koinInjectUseCase<ConsultTeamlyticsUseCase, LeadStatsViewModel>(viewModel)
                LeadStatsTab(viewModel)
            }
            4 -> {
                val viewModel = koinInjectUseCase<ConsultTeamlyticsUseCase, MatchupNotesViewModel>(viewModel)
                MatchupNotesTab(viewModel)
            }
        }
    }
}

@Composable
private inline fun <reified USE_CASE, reified T> koinInjectUseCase(viewModel: TeamlyticsViewModel) = koinInject<T> {
    parametersOf(viewModel as USE_CASE)
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