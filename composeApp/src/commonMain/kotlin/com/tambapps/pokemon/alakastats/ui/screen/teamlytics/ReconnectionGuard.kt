package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.cloud_alert
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudError
import com.tambapps.pokemon.alakastats.infrastructure.sync.TeamSyncService
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.SnackBar
import com.tambapps.pokemon.alakastats.ui.screen.cloudsync.userMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

/**
 * Warns the user before they change the team that their cloud session expired: what they change wouldn't be synced
 * until they reconnect
 */
class ReconnectionGuard(private val syncService: TeamSyncService, private val scope: CoroutineScope) {

    /** the change the user wanted to make, waiting for their answer to the warning */
    var pendingAction by mutableStateOf<(() -> Unit)?>(null)
        private set

    var isConnecting by mutableStateOf(false)
        private set

    /**
     * Runs [action] right away, or once the user answered the warning
     */
    fun run(action: () -> Unit) {
        scope.launch {
            if (syncService.needsReconnection()) pendingAction = action
            else action()
        }
    }

    fun reconnect(snackBar: SnackBar) {
        if (isConnecting) return
        isConnecting = true
        scope.launch {
            syncService.connect()
                .onLeft { error ->
                    if (error !is CloudError.Cancelled) snackBar.show(error.userMessage(), SnackBar.Severity.ERROR)
                }
                .onRight {
                    snackBar.show("Google Drive connected", SnackBar.Severity.SUCCESS)
                    continueAnyway()
                }
            isConnecting = false
        }
    }

    fun continueAnyway() {
        val action = pendingAction ?: return
        pendingAction = null
        action()
    }

    fun cancel() {
        pendingAction = null
    }
}

/** null outside of the team screen: the changes are then made without warning */
val LocalReconnectionGuard = staticCompositionLocalOf<ReconnectionGuard?> { null }

@Composable
fun rememberReconnectionGuard(): ReconnectionGuard {
    val syncService = koinInject<TeamSyncService>()
    val scope = rememberCoroutineScope()
    return remember { ReconnectionGuard(syncService, scope) }
}

/**
 * Gives a function to call with each change of the team, which warns the user first if their cloud session expired
 */
@Composable
fun rememberGuardedChange(): (() -> Unit) -> Unit {
    val guard = LocalReconnectionGuard.current
    return remember(guard) {
        { action -> if (guard != null) guard.run(action) else action() }
    }
}

@Composable
fun ReconnectionDialog(guard: ReconnectionGuard) {
    if (guard.pendingAction == null) return
    val snackBar = LocalSnackBar.current
    AlertDialog(
        onDismissRequest = { if (!guard.isConnecting) guard.cancel() },
        icon = { Icon(painterResource(Res.drawable.cloud_alert), contentDescription = null) },
        title = { Text("Google Drive session expired") },
        text = { Text("What you change now won't be synced to your Google Drive until you reconnect.") },
        confirmButton = {
            Button(onClick = { guard.reconnect(snackBar) }, enabled = !guard.isConnecting) {
                if (guard.isConnecting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Reconnect")
            }
        },
        dismissButton = {
            TextButton(onClick = { guard.continueAnyway() }, enabled = !guard.isConnecting) {
                Text("Continue anyway")
            }
        },
    )
}
