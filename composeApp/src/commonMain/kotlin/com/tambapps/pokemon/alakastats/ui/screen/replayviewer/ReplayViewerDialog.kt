package com.tambapps.pokemon.alakastats.ui.screen.replayviewer

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.close
import alakastats.composeapp.generated.resources.open_in_new
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import arrow.core.Either
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.infrastructure.service.ReplayAnalyticsService
import com.tambapps.pokemon.alakastats.ui.composables.HtmlView
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.theme.isDarkThemeEnabled
import com.tambapps.pokemon.alakastats.ui.theme.defaultIconColor
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

// the height of the dialog's body until the replay tells its own
private val LOADING_HEIGHT = 200.dp
// Showdown's battle, 640px and its border: below it, the battle scales down
private val BATTLE_WIDTH = 642.dp
// the battle and its log on its right
private val BATTLE_WITH_LOG_WIDTH = 1200.dp

/**
 * Plays a Showdown replay back in the app
 * @param replayUrl the replay's URL, as stored
 * @param browserUrl the replay's page on Showdown, to open it in the browser instead
 * @param asP2 whether to show the battle from the second player's side
 */
@Composable
fun ReplayViewerDialog(
    replayUrl: String,
    browserUrl: String,
    asP2: Boolean,
    onDismissRequest: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val replayService = koinInject<ReplayAnalyticsService>()
    val log by produceState<Either<DomainError, String>?>(null, replayUrl) {
        value = replayService.fetchLog(replayUrl)
    }
    // on desktop, the log fits on the right of the battle. On mobile, it is hidden at first, the battle alone being
    // enough to follow it
    val isCompact = LocalIsCompact.current
    val player = remember { ReplayPlayer(showLog = !isCompact) }

    // the platform's default width is too narrow for the battle
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints {
            Surface(
                // as wide as the battle, with its log on its right when shown, the page putting it below when it
                // doesn't fit. As high as its content, which scrolls beyond
                modifier = Modifier.fillMaxWidth(0.95f)
                    .widthIn(max = if (player.showLog) BATTLE_WITH_LOG_WIDTH else BATTLE_WIDTH)
                    .heightIn(max = maxHeight * 0.9f),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Replay", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        HeaderIconButton(Res.drawable.open_in_new, "Open in browser") { uriHandler.openUri(browserUrl) }
                        HeaderIconButton(Res.drawable.close, "Close", onClick = onDismissRequest)
                    }
                    when (val result = log) {
                        null -> Box(Modifier.fillMaxWidth().height(LOADING_HEIGHT), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        is Either.Left -> Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Text("Couldn't load the replay: ${result.value.message}", textAlign = TextAlign.Center)
                            OutlinedButton(onClick = { uriHandler.openUri(browserUrl) }) {
                                Text("Open in browser")
                            }
                        }
                        is Either.Right -> {
                            var obscured by remember { mutableStateOf(false) }
                            val dark = isDarkThemeEnabled()
                            // toggling the log changes the loaded page, not this one, which would reload it: the
                            // page only takes it when it is reloaded for another reason
                            val html = remember(result.value, dark) {
                                replayHtml(result.value, asP2, dark, showLog = player.showLog)
                            }
                            HtmlView(
                                html = html,
                                baseUrl = REPLAY_HTML_BASE_URL,
                                bridge = player.bridge,
                                // the height of the page's content, with or without the log, or less when the
                                // dialog can't be that high
                                modifier = Modifier.fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .height(player.state?.contentHeight?.dp ?: LOADING_HEIGHT),
                                obscured = obscured,
                            )
                            ReplayControls(
                                player = player,
                                turnCount = remember(result.value) { replayTurnCount(result.value) },
                                onOverlayChange = { obscured = it },
                            )
                        }
                    }
                }
            }
        }
    }
}

// the label shows as a tooltip, an icon alone being less obvious than a text
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeaderIconButton(icon: DrawableResource, label: String, onClick: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                tint = MaterialTheme.colorScheme.defaultIconColor,
            )
        }
    }
}
