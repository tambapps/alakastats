package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.condition_spikes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.theme.isDarkThemeEnabled
import com.tambapps.pokemon.champions.engine.SideConditions
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * What a side condition does, telling them apart by color at a glance, before reading which one it is
 */
internal enum class SideConditionCategory {
    PROTECTION, BOOST, SPEED, RESIDUAL, RECOVERY
}

@Composable
private fun SideConditionCategory.color(): Color {
    val dark = isDarkThemeEnabled()
    return when (this) {
        SideConditionCategory.PROTECTION -> if (dark) Color(0xFF64B5F6) else Color(0xFF1E88E5)
        SideConditionCategory.BOOST -> if (dark) Color(0xFFFFB74D) else Color(0xFFEF6C00)
        SideConditionCategory.SPEED -> if (dark) Color(0xFF4DD0E1) else Color(0xFF00838F)
        SideConditionCategory.RESIDUAL -> if (dark) Color(0xFFE57373) else Color(0xFFC62828)
        SideConditionCategory.RECOVERY -> if (dark) Color(0xFF81C784) else Color(0xFF2E7D32)
    }
}

/**
 * A condition on for a side, with what to do to remove it
 */
internal class ActiveSideCondition(
    val text: String,
    val icon: DrawableResource,
    val category: SideConditionCategory,
    val remove: (SideConditions) -> SideConditions,
)

/**
 * The conditions on for a side, in the field panel's order
 */
internal fun SideConditions.activeConditions(): List<ActiveSideCondition> = buildList {
    SIDE_CONDITION_TOGGLES.filter { it.isActive(this@activeConditions) }.forEach {
        add(ActiveSideCondition(it.displayName, it.icon, it.category, remove = it.toggle))
    }
    if (spikesLayers > 0) {
        add(
            ActiveSideCondition(
                spikesChipText(spikesLayers), Res.drawable.condition_spikes, SideConditionCategory.RESIDUAL,
                remove = { it.copy(spikesLayers = 0) },
            )
        )
    }
}

/**
 * Badges of the conditions on for a side, shown on that side's pokemon: its icon and name, in its category's color.
 * Nothing without condition
 *
 * @param onClick what clicking a badge does, e.g. removing its condition
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SideConditionBadges(
    conditions: SideConditions,
    onClick: (ActiveSideCondition) -> Unit,
    modifier: Modifier = Modifier,
    // on the pokemon's side: the end for the defender, like its header
    alignment: Alignment.Horizontal = Alignment.Start,
) {
    val active = conditions.activeConditions()
    if (active.isEmpty()) return
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(BADGE_SPACING, alignment),
        verticalArrangement = Arrangement.spacedBy(BADGE_SPACING),
    ) {
        active.forEach { SideConditionBadge(it, onClick = { onClick(it) }) }
    }
}

@Composable
private fun SideConditionBadge(condition: ActiveSideCondition, onClick: () -> Unit) {
    val color = condition.category.color()
    Row(
        Modifier.clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.6f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // the text next to it names it
        Icon(painterResource(condition.icon), contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(condition.text, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
    }
}

private val BADGE_SPACING = 6.dp

/**
 * One dot per category of the conditions on for a side, for a hint where there is no room for the badges (e.g. a tab).
 * Nothing without condition
 */
@Composable
internal fun SideConditionDots(conditions: SideConditions, modifier: Modifier = Modifier) {
    val categories = conditions.activeConditions().map { it.category }.distinct().sorted()
    if (categories.isEmpty()) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        categories.forEach {
            Box(Modifier.size(7.dp).clip(CircleShape).background(it.color()))
        }
    }
}
