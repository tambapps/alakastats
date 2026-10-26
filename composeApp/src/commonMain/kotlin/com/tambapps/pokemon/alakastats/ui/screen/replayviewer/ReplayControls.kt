package com.tambapps.pokemon.alakastats.ui.screen.replayviewer

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.more_vert
import alakastats.composeapp.generated.resources.pause
import alakastats.composeapp.generated.resources.play_arrow
import alakastats.composeapp.generated.resources.restart_alt
import alakastats.composeapp.generated.resources.skip_next
import alakastats.composeapp.generated.resources.skip_previous
import alakastats.composeapp.generated.resources.swap_horiz
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.theme.defaultIconColor
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * The app's own controls of the replay, Showdown's being hidden
 * @param turnCount the number of turns of the replay
 * @param onOverlayChange whether the menu or the dialog of the controls is shown
 */
@Composable
fun ReplayControls(
    player: ReplayPlayer,
    turnCount: Int,
    onOverlayChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = player.state
    val enabled = state != null
    var showMenu by remember { mutableStateOf(false) }
    var showTurnDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlIconButton(Res.drawable.restart_alt, "Reset", enabled && state.started) { player.reset() }
        ControlIconButton(Res.drawable.skip_previous, "Previous turn", enabled && state.turn > 0) {
            player.previousTurn()
        }
        FilledIconButton(onClick = {
            when {
                state == null -> {}
                state.atEnd -> player.replay()
                state.paused -> player.play()
                else -> player.pause()
            }
        }, enabled = enabled) {
            val playing = state != null && !state.paused && !state.atEnd
            Icon(
                painter = painterResource(if (playing) Res.drawable.pause else Res.drawable.play_arrow),
                contentDescription = if (playing) "Pause" else "Play",
            )
        }
        ControlIconButton(Res.drawable.skip_next, "Next turn", enabled && !state.atEnd) { player.nextTurn() }
        TextButton(onClick = {
            showTurnDialog = true
            onOverlayChange(true)
        }, enabled = enabled) {
            Text("Turn ${state?.turn ?: 0} / $turnCount")
        }
        Box {
            ControlIconButton(Res.drawable.more_vert, "More", enabled) {
                showMenu = true
                onOverlayChange(true)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = {
                showMenu = false
                onOverlayChange(false)
            }) {
                DropdownMenuItem(
                    text = { Text("Switch sides") },
                    leadingIcon = { Icon(painterResource(Res.drawable.swap_horiz), contentDescription = null) },
                    onClick = {
                        player.switchSides()
                        showMenu = false
                        onOverlayChange(false)
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (player.showLog) "Hide battle log" else "Show battle log") },
                    onClick = {
                        player.toggleLog()
                        showMenu = false
                        onOverlayChange(false)
                    }
                )
                HorizontalDivider()
                DropdownMenuItem(text = { Text("Speed", style = MaterialTheme.typography.labelLarge) }, onClick = {}, enabled = false)
                ReplaySpeed.entries.forEach { speed ->
                    DropdownMenuItem(
                        text = { Text(if (speed == player.speed) "✓  ${speed.label}" else "     ${speed.label}") },
                        onClick = {
                            player.changeSpeed(speed)
                            showMenu = false
                            onOverlayChange(false)
                        }
                    )
                }
            }
        }
    }

    if (showTurnDialog) {
        GoToTurnDialog(
            turnCount = turnCount,
            onTurn = { turn -> if (turn == null) player.goToEnd() else player.goToTurn(turn) },
            onDismiss = {
                showTurnDialog = false
                onOverlayChange(false)
            }
        )
    }
}

@Composable
private fun ControlIconButton(icon: DrawableResource, description: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = if (enabled) MaterialTheme.colorScheme.defaultIconColor
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
    }
}

/**
 * @param onTurn the turn to go to, null for the end of the replay
 */
@Composable
private fun GoToTurnDialog(turnCount: Int, onTurn: (Int?) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val turn = text.toIntOrNull()?.takeIf { it in 0..turnCount }
    val goToTurn = {
        if (turn != null) {
            onTurn(turn)
            onDismiss()
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Go to turn") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit) },
                placeholder = { Text("0 to $turnCount") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { goToTurn() }),
            )
        },
        confirmButton = {
            TextButton(onClick = goToTurn, enabled = turn != null) {
                Text("Go")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    onTurn(null)
                    onDismiss()
                }) {
                    Text("End")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
