package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.notes.TeamNotesTab
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.notes.TeamNotesViewModel
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
        val TABS = listOf("Overview", "Team Notes", "Replays", "Move Usages", "Lead Stats", "Usage Stats", "Match-up Notes")
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
    }
}

@Composable
internal fun ColumnScope.Pager(
    viewModel: TeamlyticsViewModel,
    pagerState: PagerState
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.weight(1f)
    ) { page ->
        val team = viewModel.requireTeam()
        val teamState = viewModel.teamState
        when (page) {
            0 -> {
                val viewModel = koinInject<OverviewViewModel> {
                    parametersOf(teamState, team)
                }
                OverviewTab(viewModel)
            }
            1 -> {
                val viewModel = koinInject<TeamNotesViewModel> {
                    parametersOf(teamState, team)
                }
                TeamNotesTab(viewModel)
            }
            2 -> {
                val viewModel = koinInject<TeamReplayViewModel> {
                    parametersOf(teamState, team)
                }
                TeamReplayTab(viewModel)
            }
            3 -> MoveUsagesTab(viewModel)
        }
    }
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


@Composable
private fun MoveUsagesTab(viewModel: TeamlyticsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Move Usages",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Move usages content will go here",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}