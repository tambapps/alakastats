package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.usage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.composables.LinearProgressBar

@Composable
internal fun UsageStatsTabMobile(viewModel: UsageStatsViewModel) {
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        if (viewModel.isLoading) {
            return@Column
        }
        Spacer(Modifier.height(128.dp))
        UsageCard(viewModel)
        Space()
        UsageAndWinCard(viewModel)
        Space()
        TeraAndWinCard(viewModel)
    }
}

@Composable
private fun Space() = Spacer(Modifier.height(42.dp))