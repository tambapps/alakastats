package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.detail.tabs.speedscale

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    if (LocalIsCompact.current) {
        PokemonSpeedScaleViewTabMobile(viewModel, scrollState)
    } else {
        PokemonSpeedScaleViewTabDesktop(viewModel, scrollState)
    }
    ScrollToTopIfNeeded(viewModel, scrollState)
}

@Composable
internal fun SpeedScale(viewModel: PokemonSpeedScaleViewModel, scrollState: LazyListState, modifier: Modifier) {
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

val flowRowPadding @Composable get() = if (LocalIsCompact.current) 8.dp else 16.dp
val separator @Composable get() = if (LocalIsCompact.current) "\n" else " "

@Composable
fun OpposingInvestmentsFlowRow(viewModel: PokemonSpeedScaleViewModel) {
    FlowRow(
        verticalArrangement = Arrangement.Center
    ) {
        FilterChip(
            modifier = Modifier.padding(horizontal = flowRowPadding),
            onClick = { viewModel.flipMaxEvs() },
            label = {
                Text("252${separator}EVs", textAlign = TextAlign.Center)
            },
            selected = viewModel.maxEvs
        )

        FilterChip(
            modifier = Modifier.padding(horizontal = flowRowPadding),
            onClick = { viewModel.flipSpeedNature() },
            label = {
                Text("+Spe${separator}Nature", textAlign = TextAlign.Center)
            },
            selected = viewModel.speedNature
        )

        FilterChip(
            modifier = Modifier.padding(horizontal = flowRowPadding),
            onClick = { viewModel.flipScarfBoostNature() },
            label = {
                Text("Scarf/${separator}Booster Spe", textAlign = TextAlign.Center)
            },
            selected = viewModel.scarfBoost
        )

        SpeedStageDropDown(
            value = viewModel.stage,
            onValueChange = { viewModel.updateStage(it) }
        )
    }
}

@Composable
internal fun PokemonBoostsFlowRow(viewModel: PokemonSpeedScaleViewModel) {
    FlowRow(
        verticalArrangement = Arrangement.Center
    ) {
        FilterChip(
            modifier = Modifier.padding(horizontal = flowRowPadding),
            onClick = { viewModel.flipOwnScarfBoostNature() },
            label = {
                Text("Scarf/${separator}Booster Spe", textAlign = TextAlign.Center)
            },
            selected = viewModel.ownScarfBoost
        )

        SpeedStageDropDown(
            value = viewModel.ownStage,
            onValueChange = { viewModel.updateOwnStage(it) }
        )
    }
}

data class StatBoostStage(val level: Int, val multiplier: Float) {
    val displayedText get() = buildString {
        if (level == 0) append("+0 (x1)")
        else {
            if (level > 0) append("+")
            append(level)
            append(" (x")
            append(multiplier)
            append(")")
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedStageDropDown(value: Int, onValueChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val values = remember {
        listOf(
            StatBoostStage(6, 4.0f),
            StatBoostStage(5, 3.5f),
            StatBoostStage(4, 3.0f),
            StatBoostStage(3, 2.5f),
            StatBoostStage(2, 2.0f),
            StatBoostStage(1, 1.5f),
            StatBoostStage(0, 1.0f),
            StatBoostStage(-1, 0.67f),
            StatBoostStage(-2, 0.5f),
            StatBoostStage(-3, 0.4f),
            StatBoostStage(-4, 0.33f),
            StatBoostStage(-5, 0.29f),
            StatBoostStage(-6, 0.25f)
        )
    }

    ExposedDropdownMenuBox(
        modifier = Modifier.padding(horizontal = flowRowPadding),
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedButton(
            shape = FilterChipDefaults.shape,
            contentPadding = PaddingValues(horizontal = 10.dp),
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
             onClick = {}
        ) {
            Text((values.find { it.level == value } ?: values[6]).displayedText)
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            values.forEach { stage ->
                DropdownMenuItem(
                    text = { Text(stage.displayedText) },
                    onClick = {
                        onValueChange.invoke(stage.level)
                        expanded = false
                    }
                )
            }
        }
    }
}