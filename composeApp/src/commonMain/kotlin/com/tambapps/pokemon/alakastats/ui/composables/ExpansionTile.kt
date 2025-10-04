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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.theme.defaultIconColor
import org.jetbrains.compose.resources.painterResource

@Composable
fun ExpansionTile(
    title: @Composable RowScope.(Boolean) -> Unit,
    subtitle: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AbstractExpansionTile(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        content = content,
        expandButton = { isCardExpandedState ->
            ExpandButton(isCardExpandedState)
        }
    )
}


@Composable
fun ExpansionTile(
    title: @Composable RowScope.(Boolean) -> Unit,
    menu: @Composable (MutableState<Boolean>) -> Unit,
    subtitle: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AbstractExpansionTile(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        content = content,
        expandButton = { isCardExpandedState ->
            Box {
                val expandButtonScale by animateFloatAsState(
                    targetValue = if (isCardExpandedState.value) 1f else 0f
                )
                val isMenuExpanded = remember { mutableStateOf(false) }
                IconButton(onClick = { isMenuExpanded.value = !isMenuExpanded.value }, enabled = isCardExpandedState.value) {
                    Icon(
                        modifier = Modifier.scale(expandButtonScale),
                        painter = painterResource(Res.drawable.more_vert),
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.defaultIconColor
                    )
                }


                ExpandButton(isCardExpandedState)
                menu(isMenuExpanded)
            }
        }
    )
}

@Composable
private fun AbstractExpansionTile(
    modifier: Modifier,
    title: @Composable RowScope.(Boolean) -> Unit,
    subtitle: @Composable () -> Unit = {},
    expandButton: @Composable (MutableState<Boolean>) -> Unit,
    content: @Composable () -> Unit,
) {
    val isCardExpandedState = remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(),
        border = BorderStroke(
            width = 2.dp,
            color = MaterialTheme.colorScheme.outline
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        onClick = { isCardExpandedState.value = !isCardExpandedState.value }
    ) {
        Column(Modifier.padding(all = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                title(isCardExpandedState.value)
                Spacer(Modifier.weight(1f))
                expandButton(isCardExpandedState)
            }
            subtitle()
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
    isCardExpandedState: MutableState<Boolean>
    ) {
    val buttonOffset by animateDpAsState(
        targetValue = if (isCardExpandedState.value) (-48).dp else 0.dp
    )
    IconButton(
        onClick = { isCardExpandedState.value = !isCardExpandedState.value },
        modifier = Modifier.offset(x = buttonOffset)
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