package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swmansion.kmpwheelpicker.rememberWheelPickerState
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.Stat
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.ui.composables.ExpansionTile
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.PokemonWheelPicker
import com.tambapps.pokemon.alakastats.ui.composables.SelectPokemonDialog
import com.tambapps.pokemon.alakastats.ui.composables.StatBoostStageChip
import com.tambapps.pokemon.alakastats.ui.composables.SuggestionTextField
import com.tambapps.pokemon.champions.data.Ability
import com.tambapps.pokemon.champions.data.ChampionsDex
import com.tambapps.pokemon.champions.data.Item
import com.tambapps.pokemon.alakastats.ui.composables.WheelPickerDialog
import com.tambapps.pokemon.alakastats.ui.composables.elevatedCardGradientColors
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.abbreviation
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.decreasedStatColor
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.displayName
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.increasedStatColor
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.shortLabel
import com.tambapps.pokemon.alakastats.ui.service.PokemonSprite
import com.tambapps.pokemon.champions.engine.Status
import kotlin.math.roundToInt

private val hpColor = Color(0xFF4CAF50)

@Composable
internal fun DamageCalcPokemonPanel(
    state: DamageCalcPokemonState,
    side: DamageCalcSide,
    team: Teamlytics?,
    modifier: Modifier = Modifier
) {
    var showPokemonDialog by remember { mutableStateOf(false) }
    var showTeamPokemonDialog by remember { mutableStateOf(false) }
    var showFormDialog by remember { mutableStateOf(false) }
    var showNatureDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showToxicCounterDialog by remember { mutableStateOf(false) }
    var showAbilityDialog by remember { mutableStateOf(false) }
    var showItemDialog by remember { mutableStateOf(false) }
    var editedMoveIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PokemonButton(state, onClick = { showPokemonDialog = true })
        if (state.availableForms.size > 1) {
            PropertyRow("Form", state.form.pretty, onClick = { showFormDialog = true })
        }
        PropertyRow(
            label = "Ability",
            value = state.ability,
            onClick = { showAbilityDialog = true },
            // only for the abilities the source calculator has an "ability on" toggle for
            trailingContent = if (state.hasAbilityToggle) {
                {
                    FilterChip(
                        selected = state.isAbilityActive,
                        onClick = { state.isAbilityActive = !state.isAbilityActive },
                        label = { Text("Active", maxLines = 1, softWrap = false) },
                    )
                }
            } else null,
        )
        PropertyRow("Item", state.item.ifBlank { "None" }, onClick = { showItemDialog = true })
        PropertyRow("Nature", state.nature.effectDisplayName(), onClick = { showNatureDialog = true })
        StatPointsTile(state, side)
        PropertyRow("Status", state.status.displayName, onClick = { showStatusDialog = true })
        if (state.status == Status.BADLY_POISONED) {
            PropertyRow("Toxic Damage", toxicCounterText(state.toxicCounter), onClick = { showToxicCounterDialog = true })
        }
        // needed on both sides, as some moves depend on the attacker's current HP
        CurrentHpSlider(state)
        if (side == DamageCalcSide.ATTACKER) {
            state.moves.forEachIndexed { index, move ->
                val selectableHitCounts = state.selectableHitCounts(index)
                PropertyRow(
                    label = "Move ${index + 1}",
                    value = move.ifBlank { "-" },
                    onClick = { editedMoveIndex = index },
                    trailingContent = if (move.isNotBlank()) {
                        {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                CritChip(
                                    isCritical = state.isCritical(index),
                                    alwaysCrits = state.alwaysCrits(index),
                                    onCriticalChange = { state.setCritical(index, it) },
                                )
                                selectableHitCounts?.let {
                                    HitCountChip(
                                        hits = state.hitCount(index),
                                        selectableHitCounts = it,
                                        onHitCountSelected = { hits -> state.selectHitCount(index, hits) },
                                    )
                                }
                            }
                        }
                    } else null,
                )
            }
        }
    }

    if (showPokemonDialog) {
        SelectPokemonDialog(
            onSelect = { state.selectSpecies(it) },
            onDismissRequest = { showPokemonDialog = false },
            title = "Select ${side.displayName}",
            confirmButtonText = "Select",
            extraButton = if (team != null) {
                {
                    TextButton(onClick = {
                        showPokemonDialog = false
                        showTeamPokemonDialog = true
                    }) {
                        Text("From Team")
                    }
                }
            } else null,
            // forms are selected separately
            allPokemons = PICKABLE_POKEMON_NAMES,
        )
    }
    if (showFormDialog) {
        val forms = state.availableForms
        WheelPickerDialog(
            title = "Select Form",
            items = forms,
            initialIndex = forms.indexOf(state.form).coerceAtLeast(0),
            itemToText = { it.pretty },
            textAlign = TextAlign.Center,
            onPicked = { state.selectForm(it) },
            onDismissRequest = { showFormDialog = false },
        )
    }
    if (showTeamPokemonDialog && team != null) {
        SelectTeamPokemonDialog(
            team = team,
            onSelect = { state.fillFrom(it) },
            onDismissRequest = { showTeamPokemonDialog = false },
        )
    }
    if (showNatureDialog) {
        WheelPickerDialog(
            title = "Select Nature",
            items = NATURES,
            initialIndex = NATURES.indexOf(state.nature).coerceAtLeast(0),
            itemToText = { it.effectDisplayName(separator = "\n") },
            textAlign = TextAlign.Center,
            onPicked = { state.nature = it },
            onDismissRequest = { showNatureDialog = false },
        )
    }
    if (showStatusDialog) {
        WheelPickerDialog(
            title = "Select Status",
            items = Status.entries,
            initialIndex = state.status.ordinal,
            itemToText = { it.displayName },
            onPicked = { state.status = it },
            onDismissRequest = { showStatusDialog = false },
        )
    }
    if (showToxicCounterDialog) {
        val counters = TOXIC_COUNTERS.toList()
        WheelPickerDialog(
            title = "Select Toxic Damage",
            items = counters,
            initialIndex = counters.indexOf(state.toxicCounter).coerceAtLeast(0),
            itemToText = ::toxicCounterText,
            onPicked = { state.toxicCounter = it },
            onDismissRequest = { showToxicCounterDialog = false },
        )
    }
    if (showAbilityDialog) {
        EditWithSuggestionsDialog(
            title = "Edit Ability",
            label = "Ability",
            initialValue = state.ability,
            suggestions = ABILITY_NAMES,
            onSave = { state.ability = it },
            onDismissRequest = { showAbilityDialog = false },
        )
    }
    if (showItemDialog) {
        EditWithSuggestionsDialog(
            title = "Edit Item",
            label = "Item",
            initialValue = state.item,
            suggestions = ITEM_NAMES,
            onSave = { state.item = it },
            onDismissRequest = { showItemDialog = false },
            allowEmpty = true,
        )
    }
    editedMoveIndex?.let { index ->
        EditWithSuggestionsDialog(
            title = "Edit Move",
            label = "Move",
            initialValue = state.moves[index],
            suggestions = MOVE_NAMES,
            onSave = { state.setMove(index, it) },
            onDismissRequest = { editedMoveIndex = null },
        )
    }
}

@Composable
private fun SelectTeamPokemonDialog(
    team: Teamlytics,
    onSelect: (Pokemon) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val pokemons = team.pokePaste.pokemons
    val wheelState = rememberWheelPickerState(itemCount = pokemons.size, initialIndex = 0)
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select From ${team.name}") },
        text = {
            PokemonWheelPicker(
                pokemons = pokemons.map { it.name },
                state = wheelState,
            )
        },
        confirmButton = {
            TextButton(onClick = {
                pokemons.getOrNull(wheelState.index)?.let(onSelect)
                onDismissRequest.invoke()
            }) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

private val MOVE_NAMES: List<String> by lazy { ChampionsDex.allMoves.map { it.name.value }.sorted() }
private val ABILITY_NAMES: List<String> by lazy {
    Ability.entries.filter { it != Ability.NO_ABILITY }.map { it.displayName }
}
private val ITEM_NAMES: List<String> by lazy { Item.entries.map { it.displayName } }

/**
 * Dialog to edit a value with a text field suggesting the values of [suggestions]. Only a value of
 * [suggestions] can be saved, with the catalog's spelling (e.g. "moonblast" is saved as "Moonblast").
 *
 * @param allowEmpty whether an empty value can be saved (e.g. no item)
 */
@Composable
private fun EditWithSuggestionsDialog(
    title: String,
    label: String,
    initialValue: String,
    suggestions: List<String>,
    onSave: (String) -> Unit,
    onDismissRequest: () -> Unit,
    allowEmpty: Boolean = false,
) {
    var text by remember { mutableStateOf(initialValue) }
    var error: String? by remember { mutableStateOf(null) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            SuggestionTextField(
                value = text,
                onValueChange = {
                    text = it
                    error = null
                },
                suggestions = suggestions,
                suggestionText = { it },
                label = label,
                textFieldModifier = Modifier.focusRequester(focusRequester),
                isError = error != null,
                supportingText = error?.let { ({ Text(it) }) },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val input = text.trim()
                    if (input.isEmpty()) {
                        if (allowEmpty) {
                            onSave.invoke("")
                            onDismissRequest.invoke()
                        } else {
                            error = "$label cannot be empty"
                        }
                        return@TextButton
                    }
                    val value = suggestions.firstOrNull { it.equals(input, ignoreCase = true) }
                    if (value == null) {
                        error = "Unknown ${label.lowercase()}"
                        return@TextButton
                    }
                    onSave.invoke(value)
                    onDismissRequest.invoke()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

private fun Nature.effectDisplayName(separator: String = " ") =
    if (isNeutral) "$displayName$separator(Neutral)"
    else "$displayName$separator+${bonusStat?.abbreviation}/-${malusStat?.abbreviation}"

@Composable
private fun PokemonButton(state: DamageCalcPokemonState, onClick: () -> Unit) {
    MyCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        gradientBackgroundColors = elevatedCardGradientColors
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PokemonSprite(state.form, Modifier.size(64.dp))
            Spacer(Modifier.width(8.dp))
            Text(state.name.pretty, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PropertyRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    // optional content displayed after the button, e.g. a move's hit count
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp)
        )
        OutlinedButton(onClick = onClick, modifier = Modifier.weight(1f)) {
            Text(value)
        }
        if (trailingContent != null) {
            Spacer(Modifier.width(8.dp))
            trailingContent()
        }
    }
}

/**
 * Chip displaying the number of hits considered for a multi-hit move, opening a wheel picker to select another one
 */
@Composable
private fun HitCountChip(hits: Int, selectableHitCounts: IntRange, onHitCountSelected: (Int) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    FilterChip(
        selected = true,
        onClick = { showDialog = true },
        label = { Text("×$hits", maxLines = 1, softWrap = false) },
        modifier = Modifier.width(HIT_COUNT_CHIP_WIDTH),
    )
    if (showDialog) {
        val hitCounts = selectableHitCounts.toList()
        WheelPickerDialog(
            title = "Number of Hits",
            items = hitCounts,
            initialIndex = hitCounts.indexOf(hits).coerceAtLeast(0),
            itemToText = { if (it == 1) "1 hit" else "$it hits" },
            onPicked = onHitCountSelected,
            onDismissRequest = { showDialog = false },
        )
    }
}

private val HIT_COUNT_CHIP_WIDTH = 60.dp

/**
 * Chip toggling whether a move is calculated as a critical hit. Moves that always crit have it
 * selected and disabled.
 */
@Composable
private fun CritChip(isCritical: Boolean, alwaysCrits: Boolean, onCriticalChange: (Boolean) -> Unit) {
    FilterChip(
        selected = isCritical,
        enabled = !alwaysCrits,
        onClick = { onCriticalChange(!isCritical) },
        label = { Text("Crit", maxLines = 1, softWrap = false) },
    )
}

@Composable
private fun StatPointsTile(state: DamageCalcPokemonState, side: DamageCalcSide) {
    val hiddenStats = STATS.filter { it !in side.keyStats }
    ExpansionTile(
        title = { isExpanded ->
            Column(Modifier.weight(1f)) {
                Text("Stat Points", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                // so that boosts on hidden stats are not forgotten
                val hiddenBoosts = hiddenStats.filter { state.getBoost(it) != 0 }
                if (!isExpanded && hiddenBoosts.isNotEmpty()) {
                    Text(
                        hiddenBoosts.joinToString { "${boostText(state.getBoost(it))} ${it.abbreviation}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (state.exceedsMaxTotalStatPoints) {
                Text(
                    "${-state.remainingStatPoints} over",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text("${state.remainingStatPoints} left", style = MaterialTheme.typography.bodyMedium)
            }
        },
        subtitle = {
            Column {
                side.keyStats.forEach { StatPointsSlider(state, it) }
            }
        },
    ) {
        Column {
            hiddenStats.forEach { StatPointsSlider(state, it) }
        }
    }
}

private fun boostText(boost: Int) = if (boost > 0) "+$boost" else boost.toString()

@Composable
private fun StatPointsSlider(state: DamageCalcPokemonState, stat: Stat) {
    val statPoints = state.getStatPoints(stat)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stat.abbreviation, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(48.dp))
        Slider(
            value = statPoints.toFloat(),
            onValueChange = { state.setStatPoints(stat, it.roundToInt()) },
            valueRange = 0f..MAX_STAT_POINTS_PER_STAT.toFloat(),
            modifier = Modifier.weight(1f)
        )
        Text(
            statPoints.toString(),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.End,
            modifier = Modifier.width(32.dp)
        )
        // the final stat, colored when the nature changes it, "-" if the species isn't known to Champions
        Text(
            state.stats?.get(stat)?.toString() ?: "-",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            color = when (stat) {
                state.nature.bonusStat -> increasedStatColor
                state.nature.malusStat -> decreasedStatColor
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.width(FINAL_STAT_WIDTH)
        )
        Spacer(Modifier.width(8.dp))
        val boostModifier = Modifier.width(BOOST_CHIP_WIDTH)
        if (stat == Stat.HP) {
            // HP cannot be boosted, but keep the space to align the sliders
            Spacer(boostModifier)
        } else {
            val boost = state.getBoost(stat)
            StatBoostStageChip(
                stage = boost,
                onValueChange = { state.setBoost(stat, it) },
                dialogTitle = "${stat.shortLabel} Stage",
                compact = true,
                modifier = boostModifier,
                colors = FilterChipDefaults.filterChipColors(
                    selectedLabelColor = if (boost > 0) increasedStatColor else decreasedStatColor
                ),
            )
        }
    }
}

private val BOOST_CHIP_WIDTH = 60.dp
private val FINAL_STAT_WIDTH = 44.dp

// like the source calculator, e.g. "3/16"
private fun toxicCounterText(counter: Int) = "$counter/16"

@Composable
private fun CurrentHpSlider(state: DamageCalcPokemonState) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Row {
            Text("Current HP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("${state.currentHpPercent}%", style = MaterialTheme.typography.bodyLarge)
        }
        Slider(
            value = state.currentHpPercent.toFloat(),
            onValueChange = { state.currentHpPercent = it.roundToInt() },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(activeTrackColor = hpColor),
        )
    }
}
