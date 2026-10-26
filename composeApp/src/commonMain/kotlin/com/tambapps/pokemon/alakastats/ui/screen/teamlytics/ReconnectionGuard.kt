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
import com.tambapps.pokemon.alakastats.infrastructure.sync.MissingAccess
import com.tambapps.pokemon.alakastats.infrastructure.sync.TeamSyncService
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.SnackBar
import com.tambapps.pokemon.alakastats.ui.screen.cloudsync.userMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

/**
 * Warns the user before they change the team that what they change wouldn't be synced: their cloud session expired,
 * or the team is shared and this device isn't allowed to sync the shared teams
 */
class ReconnectionGuard(
    private val syncService: TeamSyncService,
    private val teamId: Uuid,
    private val scope: CoroutineScope,
) {

    /** the change the user wanted to make, waiting for their answer to the warning */
    var pendingAction by mutableStateOf<(() -> Unit)?>(null)
        private set

    /** what the user has to give for [pendingAction] to be synced */
    var missingAccess by mutableStateOf<MissingAccess?>(null)
        private set

    var isConnecting by mutableStateOf(false)
        private set

    /**
     * Runs [action] right away, or once the user answered the warning
     */
    fun run(action: () -> Unit) {
        scope.launch {
            val missing = syncService.missingAccess(teamId)
            if (missing == null) action()
            else {
                missingAccess = missing
                pendingAction = action
            }
        }
    }

    fun reconnect(snackBar: SnackBar) {
        if (isConnecting) return
        isConnecting = true
        scope.launch {
            val sharing = missingAccess == MissingAccess.SHARING
            (if (sharing) syncService.allowSharing() else syncService.connect())
                .onLeft { error ->
                    if (error !is CloudError.Cancelled) snackBar.show(error.userMessage(), SnackBar.Severity.ERROR)
                }
                .onRight {
                    snackBar.show(if (sharing) "Shared teams allowed" else "Google Drive connected", SnackBar.Severity.SUCCESS)
                    continueAnyway()
                }
            isConnecting = false
        }
    }

    fun continueAnyway() {
        val action = pendingAction ?: return
        cancel()
        action()
    }

    fun cancel() {
        pendingAction = null
        missingAccess = null
    }
}

/** null outside of the team screen: the changes are then made without warning */
val LocalReconnectionGuard = staticCompositionLocalOf<ReconnectionGuard?> { null }

@Composable
fun rememberReconnectionGuard(teamId: Uuid): ReconnectionGuard {
    val syncService = koinInject<TeamSyncService>()
    val scope = rememberCoroutineScope()
    return remember(teamId) { ReconnectionGuard(syncService, teamId, scope) }
}

/**
 * Gives a function to call with each change of the team, which warns the user first if the change wouldn't be synced
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
    val sharing = guard.missingAccess == MissingAccess.SHARING
    AlertDialog(
        onDismissRequest = { if (!guard.isConnecting) guard.cancel() },
        icon = { Icon(painterResource(Res.drawable.cloud_alert), contentDescription = null) },
        title = { Text(if (sharing) "Shared teams not allowed" else "Google Drive session expired") },
        text = {
            Text(
                if (sharing) "This team is shared, but this device isn't allowed to sync the shared teams yet. " +
                        "What you change now won't reach the other members until you allow it."
                else "What you change now won't be synced to your Google Drive until you reconnect."
            )
        },
        confirmButton = {
            Button(onClick = { guard.reconnect(snackBar) }, enabled = !guard.isConnecting) {
                if (guard.isConnecting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(if (sharing) "Allow shared teams" else "Reconnect")
            }
        },
        dismissButton = {
            TextButton(onClick = { guard.continueAnyway() }, enabled = !guard.isConnecting) {
                Text("Continue anyway")
            }
        },
    )
}
