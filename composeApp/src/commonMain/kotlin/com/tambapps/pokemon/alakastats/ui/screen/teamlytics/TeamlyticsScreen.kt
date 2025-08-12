package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.arrow_back
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

data class TeamDetailsScreen(val team: Teamlytics) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<TeamlyticsViewModel>()
        val isCompact = LocalIsCompact.current

        val tabs = listOf("Overview", "Replay Entries", "Move Usages")
        // Future tabs for testing scrolling behavior:
        // val tabs = listOf(BACK, "Overview", "Replays", "Moves", "Stats", "Matchups", "Analysis", "Settings")
        val pagerState = rememberPagerState(pageCount = { tabs.size }, initialPage = 1)

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .safeContentPadding()

        ) {
            if (isCompact) {
                TeamlyticsScreenMobile(viewModel, tabs, pagerState)
            } else {
                TeamlyticsScreenDesktop(viewModel, tabs, pagerState)
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
        when (page) {
            0 -> OverviewTab(viewModel)
            1 -> ReplayEntriesTab(viewModel)
            2 -> MoveUsagesTab(viewModel)
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
private fun OverviewTab(viewModel: TeamlyticsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Overview",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Team overview content will go here",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun ReplayEntriesTab(viewModel: TeamlyticsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Replay Entries",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Replay entries content will go here",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp)
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