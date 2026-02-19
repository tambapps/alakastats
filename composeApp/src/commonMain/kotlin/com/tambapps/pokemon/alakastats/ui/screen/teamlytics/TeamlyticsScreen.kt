package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.domain.usecase.ConsultTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageMatchupNotesUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamOverviewUseCase
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamReplaysUseCase
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.SnackBar
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.TeamlyticsTabViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.lead.LeadStatsTab
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.lead.LeadStatsViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.matchup.MatchupNotesTab
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.matchup.MatchupNotesViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.usage.UsagesTab
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.usage.UsagesViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.overview.OverviewTab
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.overview.OverviewViewModel
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.replay.TeamReplayTab
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.replay.TeamReplayViewModel
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
        val isCompact = LocalIsCompact.current
        val pagerState = rememberPagerState(pageCount = { TABS.size })

        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            when (val state = viewModel.teamState) {
                is TeamState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp).align(Alignment.Center)
                    )

                }
                is TeamState.Error -> {
                    val navigator = LocalNavigator.currentOrThrow
                    val snackBar = LocalSnackBar.current
                    LaunchedEffect(Unit) {
                        snackBar.show("Error: ${state.error.message}", SnackBar.Severity.ERROR)
                        navigator.pop()
                    }
                }
                is TeamState.Loaded -> {
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(durationMillis = 1000))
                    ) {
                        Column(Modifier.fillMaxSize()) {
                            if (isCompact) {
                                TeamlyticsScreenMobile(viewModel, TABS, pagerState)
                            } else {
                                TeamlyticsScreenDesktop(viewModel, TABS, pagerState)
                            }
                        }
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
                val viewModel = koinInjectUseCase<ManageTeamOverviewUseCase, OverviewViewModel>(viewModel, page)
                OverviewTab(viewModel)
            }
            1 -> {
                val viewModel = koinInjectUseCase<ManageTeamReplaysUseCase, TeamReplayViewModel>(viewModel, page)
                TeamReplayTab(viewModel)
            }
            2 -> {
                val viewModel = koinInjectUseCase<ConsultTeamlyticsUseCase, UsagesViewModel>(viewModel, page)
                UsagesTab(viewModel)
            }
            3 -> {
                val viewModel = koinInjectUseCase<ConsultTeamlyticsUseCase, LeadStatsViewModel>(viewModel, page)
                LeadStatsTab(viewModel)
            }
            4 -> {
                val viewModel = koinInjectUseCase<ManageMatchupNotesUseCase, MatchupNotesViewModel>(viewModel, page)
                MatchupNotesTab(viewModel)
            }
        }
    }
}

@Composable
fun ScrollToTopIfNeeded(viewModel: TeamlyticsTabViewModel, scrollState: ScrollState) {
    viewModel.scrollToTopSignal.Listen {
        if (scrollState.value != 0) {
            scrollState.animateScrollTo(
                value = 0,
                animationSpec = tween(durationMillis = 500)
            )
        }
    }
}

@Composable
fun ScrollToTopIfNeeded(viewModel: TeamlyticsTabViewModel, scrollState: LazyListState) {
    viewModel.scrollToTopSignal.Listen {
        if (scrollState.firstVisibleItemIndex != 0) {
            scrollState.animateScrollToItem(index = 0)
        }
    }
}

@Composable
private inline fun <reified USE_CASE, reified T: TeamlyticsTabViewModel> koinInjectUseCase(viewModel: TeamlyticsViewModel, index: Int) = koinInject<T> {
    parametersOf(viewModel as USE_CASE)
}.apply {
    LaunchedEffect(viewModel.scrollToTopIndex) {
        if (index == viewModel.scrollToTopIndex) {
            scrollToTopSignal.emit()
            viewModel.scrollToTopIndex = null
        }
    }
}
