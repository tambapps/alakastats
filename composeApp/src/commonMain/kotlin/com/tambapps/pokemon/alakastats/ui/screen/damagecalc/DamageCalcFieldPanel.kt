package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.champions.engine.BattleFormat
import com.tambapps.pokemon.champions.engine.Battlefield
import com.tambapps.pokemon.champions.engine.SideConditions
import com.tambapps.pokemon.champions.engine.Terrain

private class SideConditionToggle(
    val displayName: String,
    val isActive: (SideConditions) -> Boolean,
    val toggle: (SideConditions) -> SideConditions,
)

// only the conditions the damage engine takes into account. Hazards and end-of-turn effects count in the KO chance
private val SIDE_CONDITION_TOGGLES = listOf(
    SideConditionToggle("Protect", { it.isProtected }) { it.copy(isProtected = !it.isProtected) },
    SideConditionToggle("Helping Hand", { it.hasHelpingHand }) { it.copy(hasHelpingHand = !it.hasHelpingHand) },
    SideConditionToggle("Aurora Veil", { it.hasAuroraVeil }) { it.copy(hasAuroraVeil = !it.hasAuroraVeil) },
    SideConditionToggle("Reflect", { it.hasReflect }) { it.copy(hasReflect = !it.hasReflect) },
    SideConditionToggle("Light Screen", { it.hasLightScreen }) { it.copy(hasLightScreen = !it.hasLightScreen) },
    SideConditionToggle("Tailwind", { it.hasTailwind }) { it.copy(hasTailwind = !it.hasTailwind) },
    SideConditionToggle("Friend Guard", { it.hasFriendGuard }) { it.copy(hasFriendGuard = !it.hasFriendGuard) },
    SideConditionToggle("Steely Spirit", { it.hasAllySteelySpirit }) { it.copy(hasAllySteelySpirit = !it.hasAllySteelySpirit) },
    SideConditionToggle("Battery", { it.hasBattery }) { it.copy(hasBattery = !it.hasBattery) },
    SideConditionToggle("Power Spot", { it.hasPowerSpot }) { it.copy(hasPowerSpot = !it.hasPowerSpot) },
    SideConditionToggle("Stealth Rock", { it.hasStealthRock }) { it.copy(hasStealthRock = !it.hasStealthRock) },
    SideConditionToggle("Leech Seed", { it.isLeechSeeded }) { it.copy(isLeechSeeded = !it.isLeechSeeded) },
    SideConditionToggle("Salt Cure", { it.isSaltCured }) { it.copy(isSaltCured = !it.isSaltCured) },
    SideConditionToggle("Curse", { it.isCursed }) { it.copy(isCursed = !it.isCursed) },
    SideConditionToggle("Binding", { it.isBound }) { it.copy(isBound = !it.isBound) },
    // this side's pokemon used Charge, like in the source calculator it only boosts that side's Electric moves
    SideConditionToggle("Charge", { it.isCharged }) { it.copy(isCharged = !it.isCharged) },
    SideConditionToggle("Aqua Ring", { it.hasAquaRing }) { it.copy(hasAquaRing = !it.hasAquaRing) },
    SideConditionToggle("Ingrain", { it.isIngrained }) { it.copy(isIngrained = !it.isIngrained) },
)

// Spikes stack up to 3 layers: its chip cycles through 0 to 3
private const val MAX_SPIKES_LAYERS = 3

internal fun spikesChipText(layers: Int) = if (layers == 0) "Spikes" else "Spikes ×$layers"

internal fun SideConditions.withNextSpikesLayer() = copy(spikesLayers = (spikesLayers + 1) % (MAX_SPIKES_LAYERS + 1))

@Composable
internal fun DamageCalcFieldPanel(
    field: Battlefield,
    onFieldChange: (Battlefield) -> Unit,
    modifier: Modifier = Modifier,
    // the names of the sides' pokemon, e.g. "Pokémon 1" on desktop where either can attack
    sideName: (DamageCalcSide) -> String = { it.displayName },
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ChoiceSection("Format", BattleFormat.entries, field.format, { it.displayName }) { onFieldChange(field.copy(format = it)) }
        ChoiceSection("Weather", WEATHERS, field.weather, { it.displayName }) { onFieldChange(field.copy(weather = it)) }
        ChoiceSection("Terrain", Terrain.entries, field.terrain, { it.displayName }) { onFieldChange(field.copy(terrain = it)) }
        FieldSection("Global") {
            FilterChip(
                selected = field.isGravity,
                onClick = { onFieldChange(field.copy(isGravity = !field.isGravity)) },
                label = { Text("Gravity") }
            )
            FilterChip(
                selected = field.isFairyAura,
                onClick = { onFieldChange(field.copy(isFairyAura = !field.isFairyAura)) },
                label = { Text("Fairy Aura") }
            )
        }
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            SideConditionsColumn(
                title = "${sideName(DamageCalcSide.ATTACKER)} Side",
                conditions = field.attackerSide,
                onConditionsChange = { onFieldChange(field.copy(attackerSide = it)) },
                alignment = Alignment.Start,
                modifier = Modifier.weight(1f)
            )
            VerticalDivider(Modifier.padding(horizontal = 8.dp))
            SideConditionsColumn(
                title = "${sideName(DamageCalcSide.DEFENDER)} Side",
                conditions = field.defenderSide,
                onConditionsChange = { onFieldChange(field.copy(defenderSide = it)) },
                alignment = Alignment.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SideConditionsColumn(
    title: String,
    conditions: SideConditions,
    onConditionsChange: (SideConditions) -> Unit,
    alignment: Alignment.Horizontal,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = alignment) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        SIDE_CONDITION_TOGGLES.forEach { toggle ->
            FilterChip(
                selected = toggle.isActive(conditions),
                onClick = { onConditionsChange(toggle.toggle(conditions)) },
                label = { Text(toggle.displayName) }
            )
        }
        FilterChip(
            selected = conditions.spikesLayers > 0,
            onClick = { onConditionsChange(conditions.withNextSpikesLayer()) },
            label = { Text(spikesChipText(conditions.spikesLayers)) }
        )
    }
}

@Composable
private fun <T> ChoiceSection(
    title: String,
    choices: List<T>,
    selected: T,
    choiceToText: (T) -> String,
    onSelect: (T) -> Unit
) {
    FieldSection(title) {
        choices.forEach { choice ->
            FilterChip(
                selected = choice == selected,
                onClick = { onSelect(choice) },
                label = { Text(choiceToText(choice)) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FieldSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}
