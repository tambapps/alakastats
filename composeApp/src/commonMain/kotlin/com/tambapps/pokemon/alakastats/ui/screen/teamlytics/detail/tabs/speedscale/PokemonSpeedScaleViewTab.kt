package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail.tabs.speedscale

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.SnackBar
import com.tambapps.pokemon.alakastats.ui.composables.ScrollToTopIfNeeded
import com.tambapps.pokemon.alakastats.ui.service.FacingDirection


@Composable
fun PokemonSpeedScaleViewTab(
    viewModel: PokemonSpeedScaleViewModel
) {
    val snackBar = LocalSnackBar.current
    LaunchedEffect(Unit) {
        viewModel.loadSpeedScale { snackBar.show("Error while loading speed scale: ${it.message}", SnackBar.Severity.ERROR) }
    }

    if (viewModel.team.format == Format.NONE) {
        Box(Modifier.fillMaxSize()) {
            Text("Your team has no format.\nPlease edit your team to specify the format",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Center))
        }
        return
    }
    val scrollState = rememberLazyListState()
    SpeedScale(viewModel, scrollState)
    ScrollToTopIfNeeded(viewModel, scrollState)
}

@Composable
private fun SpeedScale(viewModel: PokemonSpeedScaleViewModel, scrollState: LazyListState) {
    val speedScale = viewModel.speedScale ?: return
    LazyColumn(modifier = Modifier.fillMaxSize(), state = scrollState) {
        items(speedScale.speedGroups) { pokemonSpeeds ->
            val speedValue = pokemonSpeeds.first().value
            val backgroundColor = when {
                speedValue > speedScale.interestPokemon.value -> Color(0x11FF0000)
                speedValue < speedScale.interestPokemon.value -> Color(0x1100FF00)
                else -> Color.Transparent
            }
            Column(Modifier.fillMaxSize().background(backgroundColor)) {
                FlowRow(Modifier.padding(horizontal = 8.dp)) {
                    pokemonSpeeds.forEach { pSpeed ->
                        viewModel.pokemonImageService.PokemonSprite(
                            pSpeed.pokemonName,
                            modifier = Modifier.size(64.dp).scale(1.5f),
                            facingDirection = if (pSpeed.isPokemonOfInterest) FacingDirection.LEFT else FacingDirection.RIGHT,
                            disableTooltip = false
                        )
                    }
                }

                Text(speedValue.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                    )

                HorizontalDivider(Modifier.fillMaxWidth(),
                    thickness = 4.dp,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                )
            }
        }

        item {
            Spacer(Modifier.height(128.dp))
        }
    }
}