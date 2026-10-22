package com.tambapps.pokemon.alakastats.ui.screen.home

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.add
import alakastats.composeapp.generated.resources.calculate
import alakastats.composeapp.generated.resources.cloud_alert
import alakastats.composeapp.generated.resources.cloud_done
import alakastats.composeapp.generated.resources.cloud_off
import alakastats.composeapp.generated.resources.cloud_upload
import alakastats.composeapp.generated.resources.group
import alakastats.composeapp.generated.resources.more_horiz
import alakastats.composeapp.generated.resources.quiz
import alakastats.composeapp.generated.resources.share
import alakastats.composeapp.generated.resources.visibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudError
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.tambapps.pokemon.alakastats.infrastructure.cloud.ShareRole
import com.tambapps.pokemon.alakastats.infrastructure.sync.SyncStatus
import com.tambapps.pokemon.alakastats.infrastructure.sync.TeamCloudState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.PokemonTeamPreview
import com.tambapps.pokemon.alakastats.ui.composables.elevatedCardGradientColors
import com.tambapps.pokemon.alakastats.infrastructure.sync.TeamSyncService
import com.tambapps.pokemon.alakastats.ui.screen.about.AboutScreen
import com.tambapps.pokemon.alakastats.ui.screen.cloudsync.CloudSyncScreen
import com.tambapps.pokemon.alakastats.ui.screen.damagecalc.DamageCalcScreen
import com.tambapps.pokemon.alakastats.ui.screen.editteam.EditTeamScreen
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.QuizzesHomeScreen
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

object HomeScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<HomeViewModel>()
        LaunchedEffect(Unit) {
            viewModel.loadTeams()
        }
        LaunchedEffect(Unit) {
            // a sync can add, change or delete local teams
            viewModel.teamsChangedBySync.collect { viewModel.loadTeams() }
        }
        val isCompact = LocalIsCompact.current

        if (isCompact) {
            HomeScreenMobile(viewModel)
        } else {
            HomeScreenDesktop(viewModel)
        }

        if (viewModel.isLoading) {
            Dialog(
                onDismissRequest = {},
                content = {
                    CircularProgressIndicator()
                }
            )
        }
        viewModel.teamToImport?.let { teamToImport ->
            TeamActionDialog(
                viewModel = viewModel,
                teamName = teamToImport.name,
                pokemons = teamToImport.pokePaste.pokemons.map { it.name },
                winRatePercentage = teamToImport.winRate,
                nbReplays = teamToImport.replays.size,
                title = "Load Team",
                onDismissRequest = { viewModel.dismissImportTeamDialog() },
                confirmButton = {
                    val snackBar = LocalSnackBar.current
                    TextButton(
                        onClick = { viewModel.confirmImport(snackBar) }
                    ) {
                        Text("Load")
                    }
                }
            )

        }
        val sharing by viewModel.sharing.collectAsState()
        viewModel.teamToDelete?.let { teamToDelete ->
            val role = sharing[teamToDelete.id]
            val isLeaving = role != null && role != ShareRole.OWNER
            TeamActionDialog(
                viewModel = viewModel,
                teamName = teamToDelete.name,
                pokemons = teamToDelete.pokemons,
                winRatePercentage = teamToDelete.winrate,
                nbReplays = teamToDelete.nbReplays,
                title = if (isLeaving) "Leave Team" else "Delete Team",
                text = when {
                    isLeaving -> "The team will be removed from your devices. You can join it again while it's shared with you."
                    role == ShareRole.OWNER -> "The team will be deleted for everyone it's shared with. This action cannot be undone."
                    else -> "This action cannot be undone."
                },
                onDismissRequest = { viewModel.dismissDeleteDialog() },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.confirmDelete() }
                    ) {
                        Text(if (isLeaving) "Leave" else "Delete", color = Color.Red)
                    }
                }
            )
        }
        viewModel.samplesToShow?.let { samples ->
            SamplesDialog(viewModel, samples)
        }
        ImportChoiceDialog(viewModel)
        KeepLocalDialog(viewModel)
        ShareTeamDialog(viewModel)
        JoinOnWebDialog(viewModel)
    }
}

@Composable
internal fun CatchPhrase(skipLine: Boolean = false, textAlign: TextAlign = TextAlign.Unspecified) {
    val text = buildString {
        append("Think like Alakazam.")
        append(if (skipLine) '\n' else ' ')
        append("Play like a pro.")
    }
    Text(text, style = MaterialTheme.typography.headlineMedium, textAlign = textAlign)
}

@Composable
internal fun TeamCard(viewModel: HomeViewModel, team: TeamlyticsPreview, modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.currentOrThrow

    MyCard(
        modifier = modifier,
        onClick = { viewModel.consultTeam(team, navigator) },
        gradientBackgroundColors = elevatedCardGradientColors
    ) {
        val sharing by viewModel.sharing.collectAsState()
        val role = sharing[team.id]
        val syncStatus by viewModel.syncStatus.collectAsState()
        val isCloudConnected = syncStatus !is SyncStatus.Disconnected && syncStatus !is SyncStatus.Unavailable
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                // a long name is cut, so that the icons and the menu stay on this line
                Text(
                    team.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TeamStatusIcons(viewModel, team, role)
                Box(modifier = Modifier.offset(y = (-12).dp)) {
                    IconButton(
                        onClick = { viewModel.showMenu(team.id) }
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.more_horiz),
                            contentDescription = "More"
                        )
                    }
                    DropdownMenu(
                        expanded = viewModel.expandedMenuTeamId == team.id,
                        onDismissRequest = { viewModel.hideMenu() }
                    ) {
                        val snackBar = LocalSnackBar.current
                        // a viewer of a shared team can't change it
                        if (role != ShareRole.VIEWER) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = { viewModel.editTeam(team, navigator, snackBar) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Export") },
                            onClick = { viewModel.exportTeam(team, snackBar) }
                        )
                        if (role != ShareRole.VIEWER) {
                            DropdownMenuItem(
                                text = { Text("Reload replays") },
                                onClick = { viewModel.reloadReplays(team, snackBar) }
                            )
                        }
                        // only the owner shares, and a team kept on this device only can't be
                        if (viewModel.isSharingAvailable && !team.localOnly && (role == null || role == ShareRole.OWNER)) {
                            DropdownMenuItem(
                                text = { Text(if (role == null) "Share" else "Sharing") },
                                onClick = { viewModel.shareTeam(team) }
                            )
                        }
                        // a shared team is synced by nature
                        if (isCloudConnected && role == null) {
                            if (team.localOnly) {
                                DropdownMenuItem(
                                    text = { Text("Sync with Google Drive") },
                                    onClick = { viewModel.setLocalOnly(team, localOnly = false, snackBar) }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Keep on this device only") },
                                    onClick = { viewModel.keepOnThisDeviceOnly(team) }
                                )
                            }
                        }
                        DropdownMenuItem(
                            text = { Text(if (role == null || role == ShareRole.OWNER) "Delete" else "Leave") },
                            onClick = { viewModel.deleteTeamDialog(team) }
                        )
                    }
                }
            }

            PokemonTeamPreview(team.pokemons, fillWidth = true)
            HorizontalDivider(thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
            Row {
                Text("${team.nbReplays} replays")
                Spacer(Modifier.weight(1f))
                Text("${team.winrate}% winrate")
            }
        }
    }
}

/**
 * Whether the team is shared, and where it stands with the cloud. Nothing for a team never synced
 */
@Composable
private fun TeamStatusIcons(viewModel: HomeViewModel, team: TeamlyticsPreview, role: ShareRole?) {
    val cloudStates by viewModel.teamCloudStates.collectAsState()
    val formerSharing by viewModel.formerSharing.collectAsState()
    val cloudState = cloudStates[team.id]
    // disconnected: still shared in the cloud, but not synced on this device
    val sharedRole = role ?: formerSharing[team.id]
    if (sharedRole == null && cloudState == null) return
    val navigator = LocalNavigator.currentOrThrow
    val iconSize = 20.dp
    val sharingTint = if (role != null) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    // aligned with the title's first line
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(start = 8.dp, top = 4.dp),
    ) {
        if (sharedRole == ShareRole.OWNER) {
            Icon(
                painter = painterResource(Res.drawable.share),
                contentDescription = "Shared by you",
                tint = sharingTint,
                modifier = Modifier.size(iconSize),
            )
        } else if (sharedRole != null) {
            Icon(
                painter = painterResource(Res.drawable.group),
                contentDescription = "Shared with you · ${sharedRole.label()}",
                tint = sharingTint,
                modifier = Modifier.size(iconSize),
            )
        }
        // read only: the edit actions are hidden
        if (sharedRole == ShareRole.VIEWER) {
            Icon(
                painter = painterResource(Res.drawable.visibility),
                contentDescription = "Viewer: read only",
                tint = sharingTint,
                modifier = Modifier.size(iconSize),
            )
        }
        when (cloudState) {
            TeamCloudState.DISCONNECTED -> Icon(
                painter = painterResource(Res.drawable.cloud_off),
                contentDescription = "Google Drive disconnected: its copy there isn't kept up to date",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                // the Sync screen reconnects
                modifier = Modifier.size(iconSize).clickable { navigator.push(CloudSyncScreen) },
            )
            TeamCloudState.SYNCED -> Icon(
                painter = painterResource(Res.drawable.cloud_done),
                contentDescription = "Synced",
                // discreet: the usual case
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(iconSize),
            )
            TeamCloudState.PENDING -> Icon(
                painter = painterResource(Res.drawable.cloud_upload),
                contentDescription = "Waiting to be synced",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(iconSize),
            )
            TeamCloudState.FAILED -> Icon(
                painter = painterResource(Res.drawable.cloud_alert),
                contentDescription = "Couldn't be synced",
                tint = MaterialTheme.colorScheme.error,
                // the Sync screen tells why
                modifier = Modifier.size(iconSize).clickable { navigator.push(CloudSyncScreen) },
            )
            null -> Unit
        }
    }
}

@Composable
internal fun NewTeamButton(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.currentOrThrow
    Button(
        modifier = modifier,
        onClick = { navigator.push(EditTeamScreen()) }
    ) {
        Icon(
            painter = painterResource(Res.drawable.add),
            contentDescription = "Add",
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.width(8.dp))
        Text("New Team", style = buttonTextStyle)
    }
}


internal val buttonTextStyle  @Composable get() = MaterialTheme.typography.labelLarge.copy(
    fontWeight = FontWeight.Bold
)

@Composable
internal fun ImportTeamButton(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = { viewModel.showImportChoiceDialog = true }, modifier = modifier) {
        Text("Import", style = buttonTextStyle)
    }
}

/**
 * Import from a save file, or start from a sample team
 */
@Composable
private fun ImportChoiceDialog(viewModel: HomeViewModel) {
    if (!viewModel.showImportChoiceDialog) return
    val snackBar = LocalSnackBar.current
    AlertDialog(
        onDismissRequest = { viewModel.showImportChoiceDialog = false },
        title = { Text("Import a team") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ImportChoice(
                    title = "From a file",
                    description = "An Alakastats save (.json)",
                    onClick = { viewModel.importFromFile(snackBar) },
                )
                ImportChoice(
                    title = "A sample team",
                    description = "To try the app with an example team",
                    onClick = { viewModel.chooseSample() },
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { viewModel.showImportChoiceDialog = false }) { Text("Cancel") }
        },
    )
}

@Composable
private fun KeepLocalDialog(viewModel: HomeViewModel) {
    val team = viewModel.teamToKeepLocal ?: return
    val snackBar = LocalSnackBar.current
    AlertDialog(
        onDismissRequest = { viewModel.teamToKeepLocal = null },
        title = { Text("Keep ${team.name} on this device only?") },
        text = {
            Text(
                "It will be removed from your Google Drive and from your other devices. " +
                        "You can sync it again later from its menu.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = { viewModel.setLocalOnly(team, localOnly = true, snackBar) }) { Text("Keep on this device") }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.teamToKeepLocal = null }) { Text("Cancel") }
        },
    )
}

@Composable
private fun ImportChoice(title: String, description: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun AboutButton(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.currentOrThrow
    TextButton(onClick = { navigator.push(AboutScreen) }, modifier = modifier) {
        Text("About", style = buttonTextStyle)
    }
}

/**
 * Only shown on the platforms supporting the cloud sync
 */
@Composable
internal fun CloudSyncButton(modifier: Modifier = Modifier) {
    val syncService = koinInject<TeamSyncService>()
    if (!syncService.isAvailable) return
    val navigator = LocalNavigator.currentOrThrow
    val status by syncService.status.collectAsState()
    val needsReconnection = (status as? SyncStatus.Failed)?.needsReconnection == true
    TextButton(onClick = { navigator.push(CloudSyncScreen) }, modifier = modifier) {
        Text(
            "Sync",
            style = buttonTextStyle,
            color = if (needsReconnection) MaterialTheme.colorScheme.error else Color.Unspecified,
        )
    }
}

/**
 * Tells the user that the cloud access expired or was revoked: the teams aren't synced until they reconnect
 */
@Composable
internal fun SyncReconnectionWarning(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    val status by viewModel.syncStatus.collectAsState()
    val failed = status as? SyncStatus.Failed ?: return
    if (!failed.needsReconnection) return
    val navigator = LocalNavigator.currentOrThrow
    val message = if (failed.error is CloudError.AccessRevoked) "The access to your Google Drive was lost"
    else "Your Google Drive session expired"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .clickable { navigator.push(CloudSyncScreen) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(
            painter = painterResource(Res.drawable.cloud_alert),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "$message: your teams aren't synced. Tap here to reconnect",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
internal fun QuizzesButton(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.currentOrThrow
    FilledTonalButton(onClick = { navigator.push(QuizzesHomeScreen) }, modifier = modifier) {
        Icon(
            painter = painterResource(Res.drawable.quiz),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.width(8.dp))
        Text("Quizzes", style = buttonTextStyle)
    }
}

@Composable
internal fun DamageCalcButton(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.currentOrThrow
    FilledTonalButton(onClick = { navigator.push(DamageCalcScreen) }, modifier = modifier) {
        Icon(
            painter = painterResource(Res.drawable.calculate),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.width(8.dp))
        Text("Damage Calc", style = buttonTextStyle)
    }
}

@Composable
private fun SamplesDialog(viewModel: HomeViewModel, samplePreviews: List<TeamlyticsPreview>) {
    val snackBar = LocalSnackBar.current
    AlertDialog(
        onDismissRequest = { viewModel.hideSamplesDialog() },
        title = {
            Text("Select Sample")
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                samplePreviews.forEachIndexed { index, preview ->
                    if (index > 0) {
                        HorizontalDivider(Modifier.fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 16.dp), thickness = 2.dp)
                    }
                    Column(
                        Modifier.clickable(onClick = {
                            viewModel.importSample(snackBar, preview)
                            viewModel.hideSamplesDialog()
                        })
                    ) {
                        Text(preview.name, style = MaterialTheme.typography.titleLarge)
                        PokemonTeamPreview(preview.pokemons, fillWidth = true)
                        Spacer(Modifier.height(4.dp))
                        Row {
                            Text("${preview.nbReplays} replays")
                            Spacer(Modifier.weight(1f))
                            Text("${preview.winrate}% winrate")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = { viewModel.hideSamplesDialog() }
            ) {
                Text("Cancel")
            }
        }

    )
}

@Composable
private fun TeamActionDialog(
    viewModel: HomeViewModel,
    pokemons: List<PokemonName>,
    nbReplays: Int,
    winRatePercentage: Int,
    teamName: String,
    title: String,
    text: String? = null,
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    ) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = { Column {
            text?.let {
                Text(it)
                Spacer(Modifier.height(16.dp))
            }

            Text(teamName, style = MaterialTheme.typography.titleMedium)
            PokemonTeamPreview(pokemons, fillWidth = true)
            Spacer(Modifier.height(4.dp))
            Row {
                Text("$nbReplays replays")
                Spacer(Modifier.weight(1f))
                Text("$winRatePercentage% winrate")
            }
        } },
        confirmButton = confirmButton,
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text("Cancel")
            }
        }
    )
}
