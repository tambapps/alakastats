package com.tambapps.pokemon.alakastats.ui.sitemove

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.open_in_new
import alakastats.composeapp.generated.resources.warning
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamlyticsListUseCase
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.downloadToFile
import com.tambapps.pokemon.alakastats.infrastructure.sync.TeamSyncService
import com.tambapps.pokemon.alakastats.platform
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

const val NEW_SITE_URL = "https://alakastats.com"
private val NEW_SITE_HOSTS = setOf("alakastats.com", "www.alakastats.com")
// the development server, where Google Drive has to keep working
private val LOCAL_HOSTS = setOf("localhost", "127.0.0.1")

/**
 * Whether the web app is served from an address it moved from (e.g. GitHub Pages). The browser keeps the data of each
 * address apart: the teams saved there don't follow by themselves
 */
fun isOldWebAddress(host: String?) = host != null && host !in NEW_SITE_HOSTS && host !in LOCAL_HOSTS

/**
 * The teams that won't reach the new address through Google Drive, the ones the cloud doesn't have as they are here
 *
 * @param teamsInCloud the teams whose current version is in the cloud
 */
fun teamsToMove(teams: List<TeamlyticsPreview>, teamsInCloud: Set<Uuid>) = teams.filter { it.id !in teamsInCloud }

/**
 * On an old address, a banner on top of every screen, and a dialog at each launch, push the user to move to the new
 * one, with their teams
 */
@Composable
fun SiteMoveBanner(content: @Composable () -> Unit) {
    if (!isOldWebAddress(platform.webHost)) {
        content()
        return
    }
    var showDialog by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxSize()) {
        MovedBanner(onShowHowTo = { showDialog = true })
        Box(Modifier.weight(1f)) {
            content()
        }
    }
    if (showDialog) {
        SiteMoveDialog(onDismiss = { showDialog = false })
    }
}

@Composable
private fun MovedBanner(onShowHowTo: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val isCompact = LocalIsCompact.current
    Surface(
        color = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
        modifier = Modifier.fillMaxWidth(),
    ) {
        val buttons = @Composable {
            Button(
                onClick = onShowHowTo,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onError,
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Move my teams", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { uriHandler.openUri(NEW_SITE_URL) },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onError),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onError),
            ) {
                Text("Go to alakastats.com")
                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = painterResource(Res.drawable.open_in_new),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
            }
        }
        if (isCompact) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                MovedMessage()
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { buttons() }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Box(Modifier.weight(1f)) { MovedMessage() }
                buttons()
            }
        }
    }
}

@Composable
private fun MovedMessage() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(Res.drawable.warning),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Alakastats has moved to alakastats.com", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "This address will stop working soon. Don't worry, the teams you synced with Google Drive are waiting " +
                        "for you there: connect the same account in Sync. Export the others now to import them there.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SiteMoveDialog(onDismiss: () -> Unit) {
    val useCase = koinInject<ManageTeamlyticsListUseCase>()
    val syncService = koinInject<TeamSyncService>()
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var teams by remember { mutableStateOf<List<TeamlyticsPreview>?>(null) }
    var teamsInCloud by remember { mutableStateOf(emptySet<Uuid>()) }
    LaunchedEffect(Unit) {
        teamsInCloud = syncService.teamsUpToDateInCloud()
        teams = useCase.list()
    }
    var isExporting by remember { mutableStateOf(false) }
    var exportedCount by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        // only its buttons close it: it mustn't be missed
        onDismissRequest = {},
        title = { Text("Alakastats has moved to alakastats.com") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(
                    "This address will stop working soon. Your browser keeps the teams saved here apart from the ones " +
                            "of alakastats.com: move them now so as not to lose them.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                val allTeams = teams ?: return@Column
                val toMove = teamsToMove(allTeams, teamsInCloud)
                when {
                    allTeams.isEmpty() -> Text("You have no team saved here: you can go straight to alakastats.com.")
                    else -> {
                        if (toMove.size < allTeams.size) {
                            MoveStep(
                                title = "Teams synced with Google Drive",
                                text = "Don't worry, you'll find them on alakastats.com: connect the same Google " +
                                        "account in Sync, and they'll come back by themselves.",
                            )
                        }
                        if (toMove.isNotEmpty()) {
                            MoveStep(
                                title = if (toMove.size < allTeams.size) "Teams not on Google Drive" else "Your teams",
                                text = "Export them here, then import each file on alakastats.com " +
                                        "(Import → From a file). Your browser may ask to allow several downloads.",
                            )
                            Text(
                                toMove.joinToString(separator = ", ") { it.name },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = {
                                    isExporting = true
                                    scope.launch {
                                        var count = 0
                                        toMove.forEach { preview ->
                                            useCase.get(preview.id).onRight { team ->
                                                if (downloadToFile(team.name, "json", useCase.export(team))) count++
                                            }
                                            // downloads started all at once can be dropped by the browser
                                            delay(300)
                                        }
                                        exportedCount = count
                                        isExporting = false
                                    }
                                },
                                enabled = !isExporting,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (toMove.size == 1) "Export 1 team" else "Export ${toMove.size} teams")
                            }
                            exportedCount?.let { count ->
                                Text(
                                    if (count == toMove.size) "Exported: now import them on alakastats.com."
                                    else "$count of ${toMove.size} teams exported: export the others from their menu.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { uriHandler.openUri(NEW_SITE_URL) }) {
                Text("Go to alakastats.com")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        },
    )
}

@Composable
private fun MoveStep(title: String, text: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
