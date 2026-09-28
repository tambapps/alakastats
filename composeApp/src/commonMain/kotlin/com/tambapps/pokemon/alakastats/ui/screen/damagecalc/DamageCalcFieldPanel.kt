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

@Composable
internal fun DamageCalcFieldPanel(
    field: FieldState,
    attackerSideConditions: SideConditionsState,
    defenderSideConditions: SideConditionsState,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ChoiceSection("Format", BattleFormat.entries, field.format, { it.displayName }) { field.format = it }
        ChoiceSection("Weather", Weather.entries, field.weather, { it.displayName }) { field.weather = it }
        ChoiceSection("Terrain", Terrain.entries, field.terrain, { it.displayName }) { field.terrain = it }
        FieldSection("Global") {
            FilterChip(selected = field.gravity, onClick = { field.gravity = !field.gravity }, label = { Text("Gravity") })
            FilterChip(selected = field.fairyAura, onClick = { field.fairyAura = !field.fairyAura }, label = { Text("Fairy Aura") })
        }
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            SideConditionsColumn(
                title = "Attacker Side",
                state = attackerSideConditions,
                alignment = Alignment.Start,
                modifier = Modifier.weight(1f)
            )
            VerticalDivider(Modifier.padding(horizontal = 8.dp))
            SideConditionsColumn(
                title = "Defender Side",
                state = defenderSideConditions,
                alignment = Alignment.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SideConditionsColumn(
    title: String,
    state: SideConditionsState,
    alignment: Alignment.Horizontal,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = alignment) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        SideCondition.entries.forEach { condition ->
            FilterChip(
                selected = state.isActive(condition),
                onClick = { state.toggle(condition) },
                label = { Text(condition.displayName) }
            )
        }
        // tapping cycles through 0 to MAX_SPIKES layers
        FilterChip(
            selected = state.spikes > 0,
            onClick = { state.cycleSpikes() },
            label = { Text("Spikes: ${state.spikes}") }
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
