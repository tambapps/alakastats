package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.condition_aqua_ring
import alakastats.composeapp.generated.resources.condition_aurora_veil
import alakastats.composeapp.generated.resources.condition_battery
import alakastats.composeapp.generated.resources.condition_binding
import alakastats.composeapp.generated.resources.condition_charge
import alakastats.composeapp.generated.resources.condition_curse
import alakastats.composeapp.generated.resources.condition_friend_guard
import alakastats.composeapp.generated.resources.condition_helping_hand
import alakastats.composeapp.generated.resources.condition_ingrain
import alakastats.composeapp.generated.resources.condition_leech_seed
import alakastats.composeapp.generated.resources.condition_light_screen
import alakastats.composeapp.generated.resources.condition_power_spot
import alakastats.composeapp.generated.resources.condition_protect
import alakastats.composeapp.generated.resources.condition_reflect
import alakastats.composeapp.generated.resources.condition_salt_cure
import alakastats.composeapp.generated.resources.condition_stealth_rock
import alakastats.composeapp.generated.resources.condition_steely_spirit
import alakastats.composeapp.generated.resources.condition_tailwind
import alakastats.composeapp.generated.resources.restart_alt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

internal class SideConditionToggle(
    val displayName: String,
    val icon: DrawableResource,
    val category: SideConditionCategory,
    val isActive: (SideConditions) -> Boolean,
    val toggle: (SideConditions) -> SideConditions,
)

// only the conditions the damage engine takes into account. Hazards and end-of-turn effects count in the KO chance
internal val SIDE_CONDITION_TOGGLES = listOf(
    SideConditionToggle("Protect", Res.drawable.condition_protect, SideConditionCategory.PROTECTION, { it.isProtected }) {
        it.copy(isProtected = !it.isProtected)
    },
    SideConditionToggle("Helping Hand", Res.drawable.condition_helping_hand, SideConditionCategory.BOOST, { it.hasHelpingHand }) {
        it.copy(hasHelpingHand = !it.hasHelpingHand)
    },
    SideConditionToggle("Aurora Veil", Res.drawable.condition_aurora_veil, SideConditionCategory.PROTECTION, { it.hasAuroraVeil }) {
        it.copy(hasAuroraVeil = !it.hasAuroraVeil)
    },
    SideConditionToggle("Reflect", Res.drawable.condition_reflect, SideConditionCategory.PROTECTION, { it.hasReflect }) {
        it.copy(hasReflect = !it.hasReflect)
    },
    SideConditionToggle("Light Screen", Res.drawable.condition_light_screen, SideConditionCategory.PROTECTION, { it.hasLightScreen }) {
        it.copy(hasLightScreen = !it.hasLightScreen)
    },
    SideConditionToggle("Tailwind", Res.drawable.condition_tailwind, SideConditionCategory.SPEED, { it.hasTailwind }) {
        it.copy(hasTailwind = !it.hasTailwind)
    },
    SideConditionToggle("Friend Guard", Res.drawable.condition_friend_guard, SideConditionCategory.PROTECTION, { it.hasFriendGuard }) {
        it.copy(hasFriendGuard = !it.hasFriendGuard)
    },
    SideConditionToggle("Steely Spirit", Res.drawable.condition_steely_spirit, SideConditionCategory.BOOST, { it.hasAllySteelySpirit }) {
        it.copy(hasAllySteelySpirit = !it.hasAllySteelySpirit)
    },
    SideConditionToggle("Battery", Res.drawable.condition_battery, SideConditionCategory.BOOST, { it.hasBattery }) {
        it.copy(hasBattery = !it.hasBattery)
    },
    SideConditionToggle("Power Spot", Res.drawable.condition_power_spot, SideConditionCategory.BOOST, { it.hasPowerSpot }) {
        it.copy(hasPowerSpot = !it.hasPowerSpot)
    },
    SideConditionToggle("Stealth Rock", Res.drawable.condition_stealth_rock, SideConditionCategory.RESIDUAL, { it.hasStealthRock }) {
        it.copy(hasStealthRock = !it.hasStealthRock)
    },
    SideConditionToggle("Leech Seed", Res.drawable.condition_leech_seed, SideConditionCategory.RESIDUAL, { it.isLeechSeeded }) {
        it.copy(isLeechSeeded = !it.isLeechSeeded)
    },
    SideConditionToggle("Salt Cure", Res.drawable.condition_salt_cure, SideConditionCategory.RESIDUAL, { it.isSaltCured }) {
        it.copy(isSaltCured = !it.isSaltCured)
    },
    SideConditionToggle("Curse", Res.drawable.condition_curse, SideConditionCategory.RESIDUAL, { it.isCursed }) {
        it.copy(isCursed = !it.isCursed)
    },
    SideConditionToggle("Binding", Res.drawable.condition_binding, SideConditionCategory.RESIDUAL, { it.isBound }) {
        it.copy(isBound = !it.isBound)
    },
    // this side's pokemon used Charge, like in the source calculator it only boosts that side's Electric moves
    SideConditionToggle("Charge", Res.drawable.condition_charge, SideConditionCategory.BOOST, { it.isCharged }) {
        it.copy(isCharged = !it.isCharged)
    },
    SideConditionToggle("Aqua Ring", Res.drawable.condition_aqua_ring, SideConditionCategory.RECOVERY, { it.hasAquaRing }) {
        it.copy(hasAquaRing = !it.hasAquaRing)
    },
    SideConditionToggle("Ingrain", Res.drawable.condition_ingrain, SideConditionCategory.RECOVERY, { it.isIngrained }) {
        it.copy(isIngrained = !it.isIngrained)
    },
)

// Spikes stack up to 3 layers: its chip cycles through 0 to 3
private const val MAX_SPIKES_LAYERS = 3

internal fun spikesChipText(layers: Int) = if (layers == 0) "Spikes" else "Spikes ×$layers"

internal fun SideConditions.withNextSpikesLayer() = copy(spikesLayers = (spikesLayers + 1) % (MAX_SPIKES_LAYERS + 1))

@Composable
internal fun DamageCalcFieldPanel(
    field: Battlefield,
    onFieldChange: (Battlefield) -> Unit,
    // resets the field's conditions, enabled when canReset
    onReset: () -> Unit,
    canReset: Boolean,
    modifier: Modifier = Modifier,
    // the names of the sides' pokemon, e.g. "Pokémon 1" on desktop where either can attack
    sideName: (DamageCalcSide) -> String = { it.displayName },
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // on top, as it resets almost the whole panel
        OutlinedButton(onClick = onReset, enabled = canReset, modifier = Modifier.align(Alignment.End)) {
            Icon(painterResource(Res.drawable.restart_alt), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Reset Field", maxLines = 1, softWrap = false)
        }
        ChoiceSection("Format", BattleFormat.entries, field.format, { it.displayName }) { onFieldChange(field.copy(format = it)) }
        ChoiceSection(
            "Weather", WEATHERS, field.weather, { it.displayName },
            choiceIcon = { WeatherIcon(it, Modifier.size(FilterChipDefaults.IconSize)) },
        ) { onFieldChange(field.copy(weather = it)) }
        ChoiceSection(
            "Terrain", Terrain.entries, field.terrain, { it.displayName },
            choiceIcon = { TerrainIcon(it, Modifier.size(FilterChipDefaults.IconSize)) },
        ) { onFieldChange(field.copy(terrain = it)) }
        FieldSection("Global") {
            FieldChip(
                selected = field.isGravity,
                onClick = { onFieldChange(field.copy(isGravity = !field.isGravity)) },
                text = "Gravity",
            )
            FieldChip(
                selected = field.isFairyAura,
                onClick = { onFieldChange(field.copy(isFairyAura = !field.isFairyAura)) },
                text = "Fairy Aura",
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
            FieldChip(
                selected = toggle.isActive(conditions),
                onClick = { onConditionsChange(toggle.toggle(conditions)) },
                text = toggle.displayName,
            )
        }
        FieldChip(
            selected = conditions.spikesLayers > 0,
            onClick = { onConditionsChange(conditions.withNextSpikesLayer()) },
            text = spikesChipText(conditions.spikesLayers),
        )
    }
}

@Composable
private fun <T> ChoiceSection(
    title: String,
    choices: List<T>,
    selected: T,
    choiceToText: (T) -> String,
    // an icon before the text, for the choices having one (e.g. not "None")
    choiceIcon: (@Composable (T) -> Unit)? = null,
    onSelect: (T) -> Unit
) {
    FieldSection(title) {
        choices.forEach { choice ->
            FieldChip(
                selected = choice == selected,
                onClick = { onSelect(choice) },
                text = choiceToText(choice),
                leadingIcon = choiceIcon?.let { { it(choice) } },
            )
        }
    }
}

/**
 * A chip of the panel, telling the selected ones apart over the field effects drawn behind: opaque when not selected,
 * in the theme's dark gold with a gold border when selected (dark enough for the effects' colored icons)
 */
@Composable
private fun FieldChip(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, fontWeight = if (selected) FontWeight.Bold else null) },
        leadingIcon = leadingIcon,
        colors = calcChipColors(),
        border = calcChipBorder(selected),
    )
}

/**
 * The colors of the calc's toggle chips (the field panel's, the ability's "Active"): opaque when not selected, in the
 * theme's dark gold (primaryContainer) when selected
 */
@Composable
internal fun calcChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surface,
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

/**
 * The border of the calc's toggle chips, see [calcChipColors]: gold when selected
 */
@Composable
internal fun calcChipBorder(selected: Boolean) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    selectedBorderColor = MaterialTheme.colorScheme.primary,
    selectedBorderWidth = 1.dp,
)

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
