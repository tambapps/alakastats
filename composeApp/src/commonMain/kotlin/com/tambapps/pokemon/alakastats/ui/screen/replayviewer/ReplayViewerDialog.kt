package com.tambapps.pokemon.alakastats.ui.screen.replayviewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.tambapps.pokemon.alakastats.ui.theme.isDarkThemeEnabled
import org.koin.compose.koinInject

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

    // the platform's default width is too narrow for the battle
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            // the width of the battle and its log side by side
            modifier = Modifier.fillMaxWidth(0.95f).widthIn(max = 1200.dp).fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Replay", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { uriHandler.openUri(browserUrl) }) {
                        Text("Open in browser")
                    }
                    TextButton(onClick = onDismissRequest) {
                        Text("Close")
                    }
                }
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    when (val result = log) {
                        null -> CircularProgressIndicator()
                        is Either.Left -> Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text("Couldn't load the replay: ${result.value.message}", textAlign = TextAlign.Center)
                            OutlinedButton(onClick = { uriHandler.openUri(browserUrl) }) {
                                Text("Open in browser")
                            }
                        }
                        is Either.Right -> Column(Modifier.fillMaxSize()) {
                            val player = remember { ReplayPlayer() }
                            var obscured by remember { mutableStateOf(false) }
                            HtmlView(
                                html = replayHtml(result.value, asP2, dark = isDarkThemeEnabled()),
                                baseUrl = REPLAY_HTML_BASE_URL,
                                bridge = player.bridge,
                                modifier = Modifier.fillMaxWidth().weight(1f),
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
