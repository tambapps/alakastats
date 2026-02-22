package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail.tabs.speedscale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.SnackBar
import com.tambapps.pokemon.alakastats.ui.composables.LinearProgressBarIfEnabled
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact


@Composable
fun PokemonSpeedScaleViewTab(
    viewModel: PokemonSpeedScaleViewModel
) {
    val snackBar = LocalSnackBar.current
    LaunchedEffect(Unit) {
        viewModel.loadSpeedScale { snackBar.show("Error while loading speed scale: ${it.message}", SnackBar.Severity.ERROR) }
    }

}