package com.tambapps.pokemon.alakastats.ui.screen.cloudsync

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudError
import com.tambapps.pokemon.alakastats.infrastructure.sync.SyncStatus
import com.tambapps.pokemon.alakastats.infrastructure.sync.TeamSyncService
import com.tambapps.pokemon.alakastats.ui.SnackBar
import kotlinx.coroutines.launch

class CloudSyncViewModel(
    private val syncService: TeamSyncService,
) : ScreenModel {

    val status = syncService.status

    var isConnecting by mutableStateOf(false)
        private set

    fun connect(snackBar: SnackBar) {
        if (isConnecting) return
        isConnecting = true
        screenModelScope.launch {
            syncService.connect()
                .onLeft { error ->
                    if (error !is CloudError.Cancelled) snackBar.show(error.userMessage(), SnackBar.Severity.ERROR)
                }
                .onRight { snackBar.show("Google Drive connected", SnackBar.Severity.SUCCESS) }
            isConnecting = false
        }
    }

    fun disconnect(snackBar: SnackBar) {
        screenModelScope.launch {
            syncService.disconnect()
            snackBar.show("Google Drive disconnected. Your teams are still on this device")
        }
    }

    fun syncNow(snackBar: SnackBar) {
        screenModelScope.launch {
            when (val status = syncService.sync()) {
                is SyncStatus.Failed -> snackBar.show(status.error.userMessage(), SnackBar.Severity.ERROR)
                is SyncStatus.Idle -> if (status.failures.isNotEmpty()) {
                    snackBar.show("${status.failures.size} team(s) couldn't be synced, see why below", SnackBar.Severity.WARNING)
                } else snackBar.show("Teams synced", SnackBar.Severity.SUCCESS)
                else -> Unit
            }
        }
    }
}

fun CloudError.userMessage(): String = when (this) {
    is CloudError.Network -> "Couldn't reach Google Drive. Check your connection and try again. Your changes are kept on this device and will be synced later"
    is CloudError.StorageFull -> "Your Google Drive is full. Free some space for the sync to work again"
    is CloudError.AccessRevoked -> "The access to Google Drive was lost. Please reconnect"
    // e.g. on the web, where the access expires after an hour
    is CloudError.NeedsUserInteraction -> "Your Google Drive session expired. Reconnect to sync"
    is CloudError.RateLimited -> "Google Drive is busy. Try again in a few minutes"
    is CloudError.Cancelled -> "Connection cancelled"
    is CloudError.NotFound, is CloudError.Unknown -> "Sync failed: $message"
}
