package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
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
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.Stat
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.ui.composables.ExpansionTile
import com.tambapps.pokemon.alakastats.ui.composables.MegaSwitch
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.PokemonWheelPickerDialog
import com.tambapps.pokemon.alakastats.ui.composables.SelectPokemonDialog
import com.tambapps.pokemon.alakastats.ui.composables.StatBoostStageChip
import com.tambapps.pokemon.alakastats.ui.composables.SuggestionTextField
import com.tambapps.pokemon.champions.data.Ability
import com.tambapps.pokemon.champions.data.ChampionsDex
import com.tambapps.pokemon.champions.data.Item
import com.tambapps.pokemon.alakastats.ui.composables.WheelPickerDialog
import com.tambapps.pokemon.alakastats.ui.composables.elevatedCardGradientColors
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.abbreviation
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.displayName
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.shortLabel
import com.tambapps.pokemon.alakastats.ui.service.FacingDirection
import com.tambapps.pokemon.alakastats.ui.service.PokemonSprite
import com.tambapps.pokemon.champions.engine.Battlefield
import com.tambapps.pokemon.champions.engine.Status
import com.tambapps.pokemon.champions.engine.returnsDefenderMove
import kotlin.math.roundToInt

private val hpColor = Color(0xFF4CAF50)

// the team's buttons, under the pokemon button
private val TEAM_BUTTON_HEIGHT = 52.dp
private val TEAM_SPRITE_SIZE = 44.dp
private val TEAM_BUTTONS_SPACING = 6.dp

@Composable
internal fun DamageCalcPokemonPanel(
    state: DamageCalcPokemonState,
    side: DamageCalcSide,
    team: Teamlytics?,
    // the other pokemon of the calc, whose moves Counter-like moves return
    opponent: DamageCalcPokemonState,
    // the field of the calc, for the final speed
    field: Battlefield,
    modifier: Modifier = Modifier
) {
    var showPokemonDialog by remember { mutableStateOf(false) }
    var showFormDialog by remember { mutableStateOf(false) }
    var showNatureDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showToxicCounterDialog by remember { mutableStateOf(false) }
    var showAbilityDialog by remember { mutableStateOf(false) }
    var showItemDialog by remember { mutableStateOf(false) }
    var editedMoveIndex by remember { mutableStateOf<Int?>(null) }
    var showRivalryDialog by remember { mutableStateOf(false) }
    var showFaintedAlliesDialog by remember { mutableStateOf(false) }
    var counteredMoveDialogIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // a pokemon holding its mega stone has a mega switch instead of a form selection: right of the attacker,
            // left of the defender
            val showsMegaSwitch = state.megaFormOfItem != null
            if (showsMegaSwitch && side == DamageCalcSide.DEFENDER) {
                MegaSwitch(state.isMegaEvolved, onCheckedChange = { state.setMegaEvolved(it) })
            }
            Box(Modifier.weight(1f)) {
                PokemonButton(state, side, onClick = { showPokemonDialog = true })
            }
            if (showsMegaSwitch && side == DamageCalcSide.ATTACKER) {
                MegaSwitch(state.isMegaEvolved, onCheckedChange = { state.setMegaEvolved(it) })
            }
        }
        // the team's pokemon, to fill one in a single tap, sharing the width equally
        if (team != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(TEAM_BUTTONS_SPACING)) {
                team.pokePaste.pokemons.forEach { pokemon ->
                    TeamPokemonButton(
                        pokemon, state, side,
                        spriteSize = TEAM_SPRITE_SIZE,
                        modifier = Modifier.weight(1f).height(TEAM_BUTTON_HEIGHT),
                    )
                }
            }
        }
        if (state.availableForms.size > 1 && state.megaFormOfItem == null) {
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
        // the ability settings the source calculator shows for these abilities only
        when (state.resolvedAbility) {
            Ability.RIVALRY -> PropertyRow("Rivalry", state.rivalry.displayName, onClick = { showRivalryDialog = true })
            Ability.SUPREME_OVERLORD -> PropertyRow(
                "Fainted Allies",
                faintedAlliesText(state.faintedAllyCount),
                onClick = { showFaintedAlliesDialog = true }
            )
            else -> {}
        }
        PropertyRow("Item", state.item.ifBlank { "None" }, onClick = { showItemDialog = true })
        PropertyRow("Nature", state.nature.effectDisplayName(), onClick = { showNatureDialog = true })
        StatPointsTile(state, side, finalSpeed = state.finalSpeed(field, side))
        PropertyRow("Status", state.status.displayName, onClick = { showStatusDialog = true })
        if (state.status == Status.BADLY_POISONED) {
            PropertyRow("Toxic Damage", toxicCounterText(state.toxicCounter), onClick = { showToxicCounterDialog = true })
        }
        // needed on both sides, as some moves depend on the attacker's current HP
        CurrentHpSlider(state)
        // on both sides for symmetry: the defender's moves are the ones calculated after a swap
        state.moves.forEachIndexed { index, move ->
            val selectableHitCounts = state.selectableHitCounts(index)
            val championsMove = state.championsMove(index)
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
                            if (championsMove?.canBePowerDoubled == true) {
                                FilterChip(
                                    selected = state.isPowerDoubled(index),
                                    onClick = { state.setPowerDoubled(index, !state.isPowerDoubled(index)) },
                                    label = { Text("2x BP", maxLines = 1, softWrap = false) },
                                )
                            }
                            if (championsMove?.hasStackingPower == true) {
                                StackCountChip(
                                    moveName = championsMove.name.value,
                                    count = state.stackCount(index),
                                    onCountSelected = { state.setStackCount(index, it) },
                                )
                            }
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
            // Counter-like moves return one of the opponent's moves, picked like in the source calculator
            if (championsMove?.returnsDefenderMove == true) {
                PropertyRow(
                    label = "Returns",
                    value = opponent.moves.getOrNull(state.counteredMoveIndex(index))?.ifBlank { null } ?: "-",
                    onClick = { counteredMoveDialogIndex = index },
                )
            }
        }
    }

    if (showRivalryDialog) {
        WheelPickerDialog(
            title = "Rivalry",
            items = RivalryRelation.entries,
            initialIndex = state.rivalry.ordinal,
            itemToText = { it.displayName },
            onPicked = { state.rivalry = it },
            onDismissRequest = { showRivalryDialog = false },
        )
    }
    if (showFaintedAlliesDialog) {
        val counts = FAINTED_ALLY_COUNTS.toList()
        WheelPickerDialog(
            title = "Fainted Allies",
            items = counts,
            initialIndex = counts.indexOf(state.faintedAllyCount).coerceAtLeast(0),
            itemToText = ::faintedAlliesText,
            onPicked = { state.faintedAllyCount = it },
            onDismissRequest = { showFaintedAlliesDialog = false },
        )
    }
    counteredMoveDialogIndex?.let { index ->
        val opponentMoveIndexes = opponent.moves.indices.toList()
        WheelPickerDialog(
            title = "Returned Move",
            items = opponentMoveIndexes,
            initialIndex = state.counteredMoveIndex(index).coerceIn(0, (opponentMoveIndexes.size - 1).coerceAtLeast(0)),
            itemToText = { opponent.moves[it].ifBlank { "-" } },
            onPicked = { state.setCounteredMoveIndex(index, it) },
            onDismissRequest = { counteredMoveDialogIndex = null },
        )
    }

    if (showPokemonDialog) {
        SelectPokemonDialog(
            onSelect = { state.selectSpecies(it) },
            onDismissRequest = { showPokemonDialog = false },
            title = "Select ${side.displayName}",
            confirmButtonText = "Select",
            // forms are selected separately
            allPokemons = PICKABLE_POKEMON_NAMES,
            onPokepasteSelect = { state.fillFrom(it) },
        )
    }
    if (showFormDialog) {
        val forms = state.availableForms
        PokemonWheelPickerDialog(
            title = "Select Form",
            pokemons = forms,
            initialIndex = forms.indexOf(state.form).coerceAtLeast(0),
            onPicked = { state.selectForm(forms[it]) },
            onDismissRequest = { showFormDialog = false },
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

internal val MOVE_NAMES: List<String> by lazy { ChampionsDex.allMoves.map { it.name.value }.sorted() }
internal val ABILITY_NAMES: List<String> by lazy {
    Ability.entries.filter { it != Ability.NO_ABILITY }.map { it.displayName }
}
internal val ITEM_NAMES: List<String> by lazy { Item.entries.map { it.displayName } }

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

internal fun Nature.effectDisplayName(separator: String = " ") =
    if (isNeutral) "$displayName$separator(Neutral)"
    else "$displayName$separator+${bonusStat?.abbreviation}/-${malusStat?.abbreviation}"

@Composable
private fun PokemonButton(state: DamageCalcPokemonState, side: DamageCalcSide, onClick: () -> Unit) {
    MyCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        gradientBackgroundColors = elevatedCardGradientColors
    ) {
        // the defender's mirrors the attacker's, the sprite on the right
        val isAttacker = side == DamageCalcSide.ATTACKER
        Row(
            Modifier.fillMaxWidth().typeTint(state, side).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isAttacker) Arrangement.Start else Arrangement.End,
        ) {
            val sprite = @Composable {
                // the attacker faces right, towards its target
                PokemonSprite(
                    state.form,
                    Modifier.size(64.dp),
                    facingDirection = if (isAttacker) FacingDirection.RIGHT else FacingDirection.LEFT,
                )
            }
            if (isAttacker) {
                sprite()
                Spacer(Modifier.width(8.dp))
            }
            Text(state.name.pretty, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (!isAttacker) {
                Spacer(Modifier.width(8.dp))
                sprite()
            }
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
            // centered also when too long for one line (e.g. a move next to its chips)
            Text(value, textAlign = TextAlign.Center)
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
internal fun HitCountChip(hits: Int, selectableHitCounts: IntRange, onHitCountSelected: (Int) -> Unit) {
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

// like the source calculator, e.g. "2 down"
internal fun faintedAlliesText(count: Int) = "$count down"

/**
 * Chip displaying how many times Last Respects/Rage Fist's effect already stacked (e.g. "3 KOs", "2 hits"),
 * opening a wheel picker to select another count
 */
@Composable
internal fun StackCountChip(moveName: String, count: Int, onCountSelected: (Int) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    FilterChip(
        selected = count > 0,
        onClick = { showDialog = true },
        label = { Text(stackCountText(moveName, count), maxLines = 1, softWrap = false) },
    )
    if (showDialog) {
        val counts = STACK_COUNTS.toList()
        WheelPickerDialog(
            title = stackCountTitle(moveName),
            items = counts,
            initialIndex = counts.indexOf(count).coerceAtLeast(0),
            itemToText = { stackCountText(moveName, it) },
            onPicked = onCountSelected,
            onDismissRequest = { showDialog = false },
        )
    }
}

// what stacks the effect: Rage Fist's power grows with the hits its user took, Last Respects' with its fainted allies
private fun stackCountText(moveName: String, count: Int) = when (moveName) {
    "Rage Fist" -> if (count == 1) "1 hit" else "$count hits"
    "Last Respects" -> if (count == 1) "1 KO" else "$count KOs"
    else -> "${count}x effect"
}

private fun stackCountTitle(moveName: String) = when (moveName) {
    "Rage Fist" -> "Times Hit"
    "Last Respects" -> "Fainted Allies"
    else -> "Effect Stacks"
}

/**
 * Chip toggling whether a move is calculated as a critical hit. Moves that always crit have it
 * selected and disabled.
 */
@Composable
internal fun CritChip(isCritical: Boolean, alwaysCrits: Boolean, onCriticalChange: (Boolean) -> Unit) {
    FilterChip(
        selected = isCritical,
        enabled = !alwaysCrits,
        onClick = { onCriticalChange(!isCritical) },
        label = { Text("Crit", maxLines = 1, softWrap = false) },
    )
}

@Composable
private fun StatPointsTile(state: DamageCalcPokemonState, side: DamageCalcSide, finalSpeed: Int?) {
    val hiddenStats = STATS.filter { it !in side.keyStats }
    ExpansionTile(
        // still expanded when coming back to its page
        saveExpandedState = true,
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
                side.keyStats.forEach { StatPointsSlider(state, it, finalSpeed) }
            }
        },
    ) {
        Column {
            hiddenStats.forEach { StatPointsSlider(state, it, finalSpeed) }
        }
    }
}

private fun boostText(boost: Int) = if (boost > 0) "+$boost" else boost.toString()

/**
 * @param finalSpeed the speed the calc uses (see [DamageCalcPokemonState.finalSpeed]), shown instead of the speed stat
 */
@Composable
internal fun StatPointsSlider(state: DamageCalcPokemonState, stat: Stat, finalSpeed: Int?) {
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
        // the final stat, "-" if the species isn't known to Champions. For the speed, the one the calc uses, colored when
        // something changes it (boosts, Choice Scarf, Tailwind...)
        val statValue = state.stats?.get(stat)
        val shownValue = if (stat == Stat.SPEED) finalSpeed ?: statValue else statValue
        Text(
            shownValue?.toString() ?: "-",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            color = if (shownValue != statValue) MaterialTheme.colorScheme.primary else Color.Unspecified,
            modifier = Modifier.width(FINAL_STAT_WIDTH)
        )
        Spacer(Modifier.width(8.dp))
        val boostModifier = Modifier.width(BOOST_CHIP_WIDTH)
        if (stat == Stat.HP) {
            // HP cannot be boosted, but keep the space to align the sliders
            Spacer(boostModifier)
        } else {
            StatBoostStageChip(
                stage = state.getBoost(stat),
                onValueChange = { state.setBoost(stat, it) },
                dialogTitle = "${stat.shortLabel} Stage",
                compact = true,
                modifier = boostModifier,
            )
        }
    }
}

private val BOOST_CHIP_WIDTH = 60.dp
private val FINAL_STAT_WIDTH = 44.dp

// like the source calculator, e.g. "3/16"
internal fun toxicCounterText(counter: Int) = "$counter/16"

@Composable
internal fun CurrentHpSlider(state: DamageCalcPokemonState) {
    val maxHp = state.maxHp
    val currentHp = state.currentHp
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Row {
            Text("Current HP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            // both the HP points and the percentage, like the source calculator's two fields
            Text(
                if (maxHp != null && currentHp != null) "$currentHp/$maxHp (${state.currentHpPercent}%)"
                else "${state.currentHpPercent}%",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        if (maxHp == null || currentHp == null) {
            // a pokemon unknown to Champions has no max HP to set exact HP points for
            Slider(
                value = state.currentHpPercent.toFloat(),
                onValueChange = { state.setCurrentHpPercent(it.roundToInt()) },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(activeTrackColor = hpColor),
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // to reach an exact HP point, hard to hit on the slider
                IconButton(onClick = { state.setCurrentHp(currentHp - 1) }, enabled = currentHp > 0) {
                    Text("−", style = MaterialTheme.typography.titleLarge)
                }
                Slider(
                    value = currentHp.toFloat(),
                    onValueChange = { state.setCurrentHp(it.roundToInt()) },
                    valueRange = 0f..maxHp.toFloat(),
                    colors = SliderDefaults.colors(activeTrackColor = hpColor),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { state.setCurrentHp(currentHp + 1) }, enabled = currentHp < maxHp) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}
