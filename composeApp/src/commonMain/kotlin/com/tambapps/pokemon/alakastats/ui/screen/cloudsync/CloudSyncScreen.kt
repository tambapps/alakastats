package com.tambapps.pokemon.alakastats.ui.screen.cloudsync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.PlatformType
import com.tambapps.pokemon.alakastats.infrastructure.sync.SyncFailure
import com.tambapps.pokemon.alakastats.infrastructure.sync.SyncStatus
import com.tambapps.pokemon.alakastats.infrastructure.sync.TeamMerge
import com.tambapps.pokemon.alakastats.platform
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton
import com.tambapps.pokemon.alakastats.ui.composables.ExpansionTile
import com.tambapps.pokemon.alakastats.ui.composables.IosSwipeBack
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

object CloudSyncScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<CloudSyncViewModel>()
        val status by viewModel.status.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val isCompact = LocalIsCompact.current
        IosSwipeBack { navigator.pop() }

        Column(
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .then(
                    if (isCompact) Modifier.safeContentPadding().padding(horizontal = 8.dp)
                    else Modifier.padding(all = 32.dp)
                ),
        ) {
            // out of the scrolled content: always visible
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                BackIconButton(navigator)
                Spacer(Modifier.width(8.dp))
                Text("Google Drive sync", style = MaterialTheme.typography.headlineMedium)
            }
            if (isCompact) {
                // the status and its buttons stay on top, the rest scrolls below
                SyncStatusCard(viewModel, status, Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Column(
                    Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SyncDetails(status)
                    HowSyncWorks(status)
                    Spacer(Modifier.height(8.dp))
                }
            } else {
                // the status and its buttons on the left, the explanation on the right, each scrolling on its own
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Column(
                        Modifier.width(400.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        SyncStatusCard(viewModel, status, Modifier.fillMaxWidth())
                        SyncDetails(status)
                    }
                    Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        HowSyncWorks(status, Modifier.widthIn(max = 800.dp))
                    }
                }
            }
        }
    }
}

/**
 * Where the sync stands, and what the user can do about it
 */
@Composable
private fun SyncStatusCard(viewModel: CloudSyncViewModel, status: SyncStatus, modifier: Modifier = Modifier) {
    val snackBar = LocalSnackBar.current
    val accountEmail by viewModel.accountEmail.collectAsState()
    ElevatedCard(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (status) {
                SyncStatus.Unavailable -> Text("Not available on this platform", style = MaterialTheme.typography.bodyLarge)
                SyncStatus.Disconnected -> {
                    Text("Not connected", style = MaterialTheme.typography.titleMedium)
                    ConnectButton(viewModel.isConnecting) { viewModel.connect(snackBar) }
                }
                is SyncStatus.Idle -> {
                    StatusText("Connected", status.lastSyncAt, accountEmail)
                    if (status.sharingNeedsConsent) {
                        ErrorText("Your shared teams can't be synced on this device until you allow it.")
                        Button(onClick = { viewModel.allowSharing(snackBar) }) { Text("Allow shared teams") }
                    }
                    ConnectedButtons(viewModel, isSyncing = false)
                }
                is SyncStatus.Syncing -> {
                    StatusText("Syncing…", status.lastSyncAt, accountEmail)
                    ConnectedButtons(viewModel, isSyncing = true)
                }
                is SyncStatus.Failed -> {
                    StatusText("Connected", status.lastSyncAt, accountEmail)
                    ErrorText(status.error.userMessage())
                    if (status.needsReconnection) {
                        ConnectButton(viewModel.isConnecting, label = "Reconnect Google Drive") { viewModel.connect(snackBar) }
                        TextButton(onClick = { viewModel.disconnect(snackBar) }) { Text("Disconnect") }
                    } else {
                        ConnectedButtons(viewModel, isSyncing = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(text, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
}

/**
 * What the last sync did, that can be long
 */
@Composable
private fun SyncDetails(status: SyncStatus) {
    if (status !is SyncStatus.Idle) return
    if (status.merges.isNotEmpty()) {
        SyncMerges(status.merges)
    }
    if (status.failures.isNotEmpty()) {
        SyncFailures(status.failures)
    }
}

/**
 * Open when not connected, to decide whether to. Once connected, it's mostly known: closed at first
 */
@Composable
private fun HowSyncWorks(status: SyncStatus, modifier: Modifier = Modifier) {
    val isConnected = status !is SyncStatus.Disconnected && status !is SyncStatus.Unavailable
    key(isConnected) {
        ExpansionTile(
            title = { Text("How sync works", style = MaterialTheme.typography.titleLarge) },
            initiallyExpanded = !isConnected,
            modifier = modifier,
        ) {
            SyncExplanation(Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun SyncExplanation(modifier: Modifier = Modifier) {
    val points = buildList {
        add("Your teams are saved in a hidden folder of your own Google Drive, that only Alakastats can access. " +
                "Connect the same Google account on your other devices to find your teams there.")
        add("Your teams stay on this device too: the app keeps working offline, and your changes are sent at the next sync.")
        add("Teams are synced when the app starts or comes back to the foreground, when you open a team, " +
                "a few seconds after each change, and when you tap \"Sync now\".")
        add("If a team was changed on two devices before a sync, both versions are merged, and you're told about it: " +
                "replays and game plans added on either side are kept, and a note edited on both sides is kept " +
                "in both versions, one after the other. For the name, the team itself and the format, the most recent change wins.")
        add("Deleting a team deletes it on all your devices, unless it was changed on another device after the deletion.")
        add("A team can be kept on this device only: when you create it, or later from its menu. It's then removed " +
                "from your Google Drive and from your other devices, until you sync it again.")
        add("You can share a team with other players (\"Share\" in its menu): it moves to an \"Alakastats\" folder of " +
                "your Google Drive, and you invite people as editors or viewers. They join it on the Alakastats website " +
                "(\"Join\" on the home screen), then it syncs on all their devices. Changes from everyone are merged the same way.")
        add("Deleting a team you shared deletes it for everyone. If you remove someone, or stop sharing, they keep a copy of the team.")
        add("Disconnecting stops the sync. Your teams stay on this device and in your Google Drive.")
        if (platform.type == PlatformType.Web) {
            add("On the web, Google only gives access for about an hour: you'll be asked to reconnect after that.")
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        points.forEach { point ->
            Text("• $point", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun SyncMerges(merges: List<TeamMerge>) {
    Column(Modifier.widthIn(max = 600.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Merged at the last sync:", style = MaterialTheme.typography.titleSmall)
        merges.forEach { merge ->
            Text("• ${merge.userMessage()}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SyncFailures(failures: List<SyncFailure>) {
    Column(Modifier.widthIn(max = 600.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "${failures.size} team(s) couldn't be synced. They'll be retried at the next sync.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleSmall,
        )
        failures.forEach { failure ->
            Text(
                "• ${failure.teamName ?: "A team from your Google Drive"}: ${failure.reason}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun StatusText(state: String, lastSyncAt: Instant?, accountEmail: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(state, style = MaterialTheme.typography.titleMedium)
        accountEmail?.let {
            Text("as $it", style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            "Last sync: " + (lastSyncAt?.let(::formatDate) ?: "never"),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ConnectButton(isConnecting: Boolean, label: String = "Connect Google Drive", onClick: () -> Unit) {
    Button(onClick = onClick, enabled = !isConnecting) {
        if (isConnecting) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Text(label)
        }
    }
}

@Composable
private fun ConnectedButtons(viewModel: CloudSyncViewModel, isSyncing: Boolean) {
    val snackBar = LocalSnackBar.current
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = { viewModel.syncNow(snackBar) }, enabled = !isSyncing) {
            if (isSyncing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text("Sync now")
        }
        OutlinedButton(onClick = { viewModel.disconnect(snackBar) }, enabled = !isSyncing) {
            Text("Disconnect")
        }
    }
}

private fun formatDate(instant: Instant): String {
    val date = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    fun Int.pad() = toString().padStart(2, '0')
    return "${date.year}-${date.month.ordinal.plus(1).pad()}-${date.day.pad()} ${date.hour.pad()}:${date.minute.pad()}"
}
