package com.tambapps.pokemon.alakastats.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.uuid.Uuid

// SPIKE: everything in this file is throwaway code, to check how the view models and the state behave

class TeamSpikeViewModel(val teamId: Uuid) : ViewModel() {
    var clicks by mutableStateOf(0)

    override fun onCleared() {
        println("[nav3-spike] view model cleared, team $teamId")
    }
}

@Composable
fun TeamSpikeScreen(teamId: Uuid) {
    val navigator = LocalAppNavigator.current
    val viewModel = koinViewModel<TeamSpikeViewModel> { parametersOf(teamId) }
    var savedClicks by rememberSaveable { mutableIntStateOf(0) }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeContentPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BackIconButton { navigator.pop() }
        Text("Route: $teamId")
        Text("View model: ${viewModel.teamId}")
        Text("Clicks in the view model: ${viewModel.clicks}, in rememberSaveable: $savedClicks")
        Button(onClick = {
            viewModel.clicks++
            savedClicks++
        }) {
            Text("Click")
        }
        Button(onClick = { navigator.push(AppRoute.Team(Uuid.random())) }) {
            Text("Open another team")
        }
    }
}

@Composable
fun NotMigratedSpikeScreen(name: String) {
    val navigator = LocalAppNavigator.current
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeContentPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BackIconButton { navigator.pop() }
        Text("$name isn't migrated in this spike")
    }
}
