package com.tambapps.pokemon.alakastats.ui.composables

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.arrow_forward
import alakastats.composeapp.generated.resources.more_vert
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.theme.defaultIconColor
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * @param saveExpandedState whether being expanded is saved (rememberSaveable), e.g. to still be expanded when coming back
 * to a pager page providing saved states
 */
@Composable
fun ExpansionTile(
    title: @Composable RowScope.(Boolean) -> Unit,
    subtitle: @Composable (Boolean) -> Unit = {},
    disableWhenOpened: Boolean = false,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    saveExpandedState: Boolean = false,
    content: @Composable () -> Unit,
) {
    AbstractExpansionTile(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        content = content,
        expandButton = { isCardExpandedState ->
            ExpandButton(isCardExpandedState, expandedOffset = 0.dp)
        },
        disableWhenOpened=disableWhenOpened,
        shrinkTitleWeightOnExpanded=false,
        initiallyExpanded=initiallyExpanded,
        saveExpandedState=saveExpandedState,
    )
}


@Composable
fun ExpansionTile(
    title: @Composable RowScope.(Boolean) -> Unit,
    menu: @Composable ((MutableState<Boolean>) -> Unit)?,
    subtitle: @Composable (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    gradientBackgroundColors: List<Color>? = null,
    onClick: (() -> Unit)? = null,
    borderColor: Color? = null,
    // e.g. off for a card holding bordered cards
    bordered: Boolean = true,
    disableWhenOpened: Boolean = false,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    AbstractExpansionTile(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        content = content,
        gradientBackgroundColors = gradientBackgroundColors,
        disableWhenOpened=disableWhenOpened,
        initiallyExpanded=initiallyExpanded,
        expandButton = { isCardExpandedState ->
            Box {
                val expandButtonScale by animateFloatAsState(
                    targetValue = if (isCardExpandedState.value) 1f else 0f
                )
                val isMenuExpanded = remember { mutableStateOf(false) }
                if (menu != null) {
                    IconButton(onClick = { isMenuExpanded.value = !isMenuExpanded.value }, enabled = isCardExpandedState.value) {
                        Icon(
                            modifier = Modifier.scale(expandButtonScale),
                            painter = painterResource(Res.drawable.more_vert),
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.defaultIconColor
                        )
                    }
                }
                ExpandButton(isCardExpandedState, expandedOffset = if (menu != null) (-48).dp else 0.dp)
                menu?.invoke(isMenuExpanded)
            }
        },
        onClick=onClick,
        borderColor=borderColor,
        bordered=bordered,
        shrinkTitleWeightOnExpanded=menu != null
    )
}

/**
 * An icon button shown next to the expand button of an opened [ExpansionTile]
 */
class ExpansionTileAction(
    val icon: DrawableResource,
    val contentDescription: String,
    val tint: Color? = null,
    val onClick: () -> Unit,
)

/**
 * A tile whose actions appear as icon buttons when it opens, the expand button sliding to their left
 */
@Composable
fun ExpansionTile(
    title: @Composable RowScope.(Boolean) -> Unit,
    actions: List<ExpansionTileAction>,
    subtitle: @Composable (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    gradientBackgroundColors: List<Color>? = null,
    disableWhenOpened: Boolean = false,
    content: @Composable () -> Unit,
) {
    AbstractExpansionTile(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        content = content,
        gradientBackgroundColors = gradientBackgroundColors,
        disableWhenOpened = disableWhenOpened,
        expandButton = { isCardExpandedState ->
            Box(contentAlignment = Alignment.CenterEnd) {
                val actionsScale by animateFloatAsState(
                    targetValue = if (isCardExpandedState.value) 1f else 0f
                )
                Row {
                    actions.forEach { action ->
                        IconButton(onClick = action.onClick, enabled = isCardExpandedState.value) {
                            Icon(
                                modifier = Modifier.scale(actionsScale),
                                painter = painterResource(action.icon),
                                contentDescription = action.contentDescription,
                                tint = action.tint ?: MaterialTheme.colorScheme.defaultIconColor
                            )
                        }
                    }
                }
                ExpandButton(isCardExpandedState, expandedOffset = (-48 * actions.size).dp)
            }
        },
        shrinkTitleWeightOnExpanded = actions.isNotEmpty(),
        // each icon button takes away some of the title's room
        expandedTitleWeight = 0.85f - 0.15f * actions.size,
    )
}

@Composable
private fun AbstractExpansionTile(
    modifier: Modifier,
    title: @Composable RowScope.(Boolean) -> Unit,
    subtitle: @Composable (Boolean) -> Unit = {},
    expandButton: @Composable (MutableState<Boolean>) -> Unit,
    gradientBackgroundColors: List<Color>? = null,
    content: @Composable () -> Unit,
    shrinkTitleWeightOnExpanded: Boolean,
    expandedTitleWeight: Float = 0.725f,
    disableWhenOpened: Boolean = false,
    onClick: (() -> Unit)? = null,
    borderColor: Color? = null,
    bordered: Boolean = true,
    initiallyExpanded: Boolean = false,
    saveExpandedState: Boolean = false,
) {
    val isCardExpandedState =
        if (saveExpandedState) rememberSaveable { mutableStateOf(initiallyExpanded) }
        else remember { mutableStateOf(initiallyExpanded) }

    MyCard(
        enabled = !disableWhenOpened || !isCardExpandedState.value,
        modifier = modifier
            .fillMaxWidth(),
        border = if (bordered) BorderStroke(
            width = 2.dp,
            color = borderColor ?: MaterialTheme.colorScheme.outline
        ) else null,
        gradientBackgroundColors = gradientBackgroundColors,
        onClick = onClick ?: { isCardExpandedState.value = !isCardExpandedState.value }
    ) {
        Column(Modifier.padding(all = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val titleWeight by animateFloatAsState(
                    targetValue = if (shrinkTitleWeightOnExpanded && isCardExpandedState.value) expandedTitleWeight else 0.85f
                )
                Row(Modifier.weight(titleWeight), verticalAlignment = Alignment.CenterVertically) {
                    title(isCardExpandedState.value)
                }
                Row(
                    modifier = Modifier.weight(1 - titleWeight).align(Alignment.Top),
                    horizontalArrangement = Arrangement.End,
                ) {
                    expandButton(isCardExpandedState)
                }
            }
            subtitle(isCardExpandedState.value)
            AnimatedVisibility(
                visible = isCardExpandedState.value,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                content()
            }
        }
    }
}

@Composable
private fun ExpandButton(
    isCardExpandedState: MutableState<Boolean>,
    // where the button slides when the tile opens, to leave room for other buttons
    expandedOffset: Dp
    ) {
    val modifier = if (expandedOffset != 0.dp) {
        val buttonOffset by animateDpAsState(
            targetValue = if (isCardExpandedState.value) expandedOffset else 0.dp
        )
        Modifier.offset(x = buttonOffset)
    } else Modifier
    IconButton(
        onClick = { isCardExpandedState.value = !isCardExpandedState.value },
        modifier = modifier
    ) {
        val rotationAngle by animateFloatAsState(
            targetValue = if (isCardExpandedState.value) 270f else 90f
        )
        Icon(
            painter = painterResource(Res.drawable.arrow_forward),
            contentDescription = "Expand/Shrink",
            modifier = Modifier.rotate(rotationAngle),
            tint = MaterialTheme.colorScheme.defaultIconColor
        )
    }
}