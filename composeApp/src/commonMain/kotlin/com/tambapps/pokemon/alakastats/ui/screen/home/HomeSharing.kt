package com.tambapps.pokemon.alakastats.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.infrastructure.cloud.ShareRole
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar

private const val WEB_APP_URL = "https://tambapps.github.io/alakastats/"

fun ShareRole.label() = when (this) {
    ShareRole.OWNER -> "Owner"
    ShareRole.EDITOR -> "Editor"
    ShareRole.VIEWER -> "Viewer"
}

@Composable
internal fun ShareTeamDialog(viewModel: HomeViewModel) {
    val state = viewModel.shareTeam
    val team = state.team ?: return
    val snackBar = LocalSnackBar.current
    val sharing by viewModel.sharing.collectAsState()
    val isShared = team.id in sharing

    AlertDialog(
        onDismissRequest = { state.close() },
        title = { Text("Share ${team.name}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Invite someone with the e-mail of their Google account. They'll get an e-mail explaining how to " +
                            "join the team on the Alakastats website. It then syncs on all their devices.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!isShared) {
                    Text(
                        "The team will be saved in an \"Alakastats\" folder of your Google Drive.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { state.email = it },
                    label = { Text("E-mail") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoleChip(state.role, ShareRole.EDITOR) { state.role = it }
                    RoleChip(state.role, ShareRole.VIEWER) { state.role = it }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { state.invite(snackBar) }, enabled = !state.isLoading && state.email.isNotBlank()) {
                        if (state.isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Invite")
                    }
                }
                Text(
                    "Editors can change the team, viewers can only look at it. Only you can invite people.",
                    style = MaterialTheme.typography.bodySmall,
                )

                val members = state.members.filter { it.role != ShareRole.OWNER }
                if (members.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Members", style = MaterialTheme.typography.titleSmall)
                    members.forEach { member ->
                        Column {
                            Text(member.email ?: member.name ?: "Someone", style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                RoleChip(member.role, ShareRole.EDITOR) { state.changeRole(member, it, snackBar) }
                                RoleChip(member.role, ShareRole.VIEWER) { state.changeRole(member, it, snackBar) }
                                TextButton(onClick = { state.remove(member, snackBar) }, enabled = !state.isLoading) {
                                    Text("Remove", color = Color.Red)
                                }
                            }
                        }
                    }
                }

                if (isShared) {
                    HorizontalDivider()
                    if (state.isConfirmingStop) {
                        Text(
                            "The members will lose the access to the team, and keep a copy of it. The team becomes private again.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { state.isConfirmingStop = false }) { Text("Cancel") }
                            Button(onClick = { state.stopSharing(snackBar) }, enabled = !state.isLoading) { Text("Stop sharing") }
                        }
                    } else {
                        TextButton(onClick = { state.isConfirmingStop = true }) { Text("Stop sharing", color = Color.Red) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { state.close() }) { Text("Close") }
        },
    )
}

@Composable
private fun RoleChip(selected: ShareRole, role: ShareRole, onSelect: (ShareRole) -> Unit) {
    FilterChip(selected = selected == role, onClick = { if (selected != role) onSelect(role) }, label = { Text(role.label()) })
}

/**
 * The file picker only works on the web: explains how to join there
 */
@Composable
internal fun JoinOnWebDialog(viewModel: HomeViewModel) {
    if (!viewModel.showJoinOnWebDialog) return
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = { viewModel.showJoinOnWebDialog = false },
        title = { Text("Join a shared team") },
        text = {
            Text(
                "To join a team someone shared with you, open Alakastats on the web, connect the same Google account " +
                        "in the Sync screen, then use \"Join\" on the home screen. The team will then appear here too.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.showJoinOnWebDialog = false
                uriHandler.openUri(WEB_APP_URL)
            }) { Text("Open the website") }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.showJoinOnWebDialog = false }) { Text("Close") }
        },
    )
}

@Composable
internal fun JoinSharedTeamButton(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    if (!viewModel.isSharingAvailable) return
    val snackBar = LocalSnackBar.current
    OutlinedButton(onClick = { viewModel.joinSharedTeam(snackBar) }, modifier = modifier) {
        Text("Join", style = buttonTextStyle)
    }
}
