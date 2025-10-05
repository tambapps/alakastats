package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.move

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact

@Composable
fun MoveUsageTab(viewModel: MoveUsageViewModel) {
    val isCompact = LocalIsCompact.current
    LaunchedEffect(Unit) {
        viewModel.loadStats()
    }
    if (!viewModel.isLoading && viewModel.pokemonMovesUsage.isEmpty()) {
        NoData()
    } else if (isCompact) {
        MoveUsageTabMobile(viewModel)
    } else {
        MoveUsageTabDesktop(viewModel)
    }
}

@Composable
private fun NoData() {
    Box(Modifier.fillMaxSize()) {
        Text("No data", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Center))
    }
}
