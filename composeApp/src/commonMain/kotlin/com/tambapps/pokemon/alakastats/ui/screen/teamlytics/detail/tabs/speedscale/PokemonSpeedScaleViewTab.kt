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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.tambapps.pokemon.alakastats.ui.composables.ExpansionTile
import com.tambapps.pokemon.alakastats.ui.composables.LazyColumnWithScrollbar
import com.tambapps.pokemon.alakastats.ui.composables.ScrollToTopIfNeeded
import com.tambapps.pokemon.alakastats.ui.service.FacingDirection
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact


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
    Column(Modifier.fillMaxSize()) {
        SettingsBar(viewModel)
        SpeedScale(viewModel, scrollState, Modifier.weight(1f))
    }
    ScrollToTopIfNeeded(viewModel, scrollState)
}

@Composable
private fun SpeedScale(viewModel: PokemonSpeedScaleViewModel, scrollState: LazyListState, modifier: Modifier) {
    val speedScale = viewModel.speedScale ?: return
    val isCompact = LocalIsCompact.current
    LazyColumnWithScrollbar(modifier = modifier.fillMaxWidth(), state = scrollState) {
        items(speedScale.speedGroups) { pokemonSpeeds ->
            val speedValue = pokemonSpeeds.first().value
            val backgroundColor = when {
                speedValue > speedScale.interestPokemon.value -> Color(0x11FF0000)
                speedValue < speedScale.interestPokemon.value -> Color(0x1100FF00)
                else -> Color.Transparent
            }
            Column(Modifier.fillMaxSize().background(backgroundColor)) {
                FlowRow(Modifier.padding(horizontal = if (isCompact) 8.dp else 32.dp)) {

                    pokemonSpeeds.forEach { pSpeed ->
                        viewModel.pokemonImageService.PokemonSprite(
                            pSpeed.pokemonName,
                            modifier = if (isCompact) Modifier.size(75.dp).scale(1.5f).padding(8.dp)
                            else Modifier.size(128.dp).scale(1.5f).padding(16.dp),
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
            Spacer(Modifier.height(256.dp))
        }
    }
}

@Composable
private fun SettingsBar(viewModel: PokemonSpeedScaleViewModel, modifier: Modifier = Modifier) {
    if (LocalIsCompact.current) {
        ExpansionTile(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            title = {
                Text("Speed Settings", style = MaterialTheme.typography.titleLarge)
            }
        ) {
            SettingsBarContent(viewModel)
        }
    } else {
        ElevatedCard(modifier = modifier.fillMaxWidth().padding(8.dp)) {
            SettingsBarContent(viewModel)
        }
    }
}

@Composable
private fun SettingsBarContent(viewModel: PokemonSpeedScaleViewModel) {
    Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text("Opposing Investments", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        val isCompact = LocalIsCompact.current
        FlowRow {
            val padding = if (isCompact) 8.dp else 16.dp
            val separator = if (isCompact) "\n" else " "
            FilterChip(
                modifier = Modifier.padding(horizontal = padding),
                onClick = { viewModel.flipMaxEvs() },
                label = {
                    Text("252${separator}EVs", textAlign = TextAlign.Center)
                },
                selected = viewModel.maxEvs
            )

            FilterChip(
                modifier = Modifier.padding(horizontal = padding),
                onClick = { viewModel.flipSpeedNature() },
                label = {
                    Text("+Spe${separator}Nature", textAlign = TextAlign.Center)
                },
                selected = viewModel.speedNature
            )

            FilterChip(
                modifier = Modifier.padding(horizontal = padding),
                onClick = { viewModel.flipScarfBoostNature() },
                label = {
                    Text("Scarf/${separator}Booster Spe", textAlign = TextAlign.Center)
                },
                selected = viewModel.scarfBoost
            )
            // TODO add speed stage ExposedDropdownMenuBox
        }

        Spacer(Modifier.height(16.dp))
        Text("${viewModel.pokemon.name.value}'s Boosts", style = MaterialTheme.typography.titleLarge)
        // TODO add
        //   - scarf enabled by default is pokemon's item is scarf or speed booster
        //   - speed stage ExposedDropdownMenuBox
    }
}
