package com.tambapps.pokemon.alakastats.ui.screen.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.infrastructure.cloud.Member
import com.tambapps.pokemon.alakastats.infrastructure.cloud.ShareRole
import com.tambapps.pokemon.alakastats.infrastructure.sync.TeamSyncService
import com.tambapps.pokemon.alakastats.ui.SnackBar
import com.tambapps.pokemon.alakastats.ui.screen.cloudsync.userMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The dialog sharing a team: invitations, members and their roles, stop sharing
 */
class ShareTeamState(
    private val syncService: TeamSyncService,
    private val scope: CoroutineScope,
) {

    var team by mutableStateOf<TeamlyticsPreview?>(null)
        private set
    var members by mutableStateOf<List<Member>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var email by mutableStateOf("")
    var role by mutableStateOf(ShareRole.EDITOR)
    var isConfirmingStop by mutableStateOf(false)

    fun open(team: TeamlyticsPreview) {
        this.team = team
        email = ""
        role = ShareRole.EDITOR
        members = emptyList()
        isConfirmingStop = false
        loadMembers(team, null)
    }

    fun close() {
        team = null
    }

    fun invite(snackBar: SnackBar) {
        val team = team ?: return
        val email = email.trim()
        if (!email.contains('@')) {
            snackBar.show("Enter the e-mail of a Google account", SnackBar.Severity.WARNING)
            return
        }
        run(snackBar) {
            syncService.shareTeam(team.id, email, role)
                .onRight {
                    this@ShareTeamState.email = ""
                    snackBar.show("Invitation sent to $email", SnackBar.Severity.SUCCESS)
                }
                .onLeft { snackBar.show(it.userMessage(), SnackBar.Severity.ERROR) }
            loadMembers(team, snackBar)
        }
    }

    fun changeRole(member: Member, role: ShareRole, snackBar: SnackBar) {
        val team = team ?: return
        run(snackBar) {
            syncService.setMemberRole(team.id, member.permissionId, role)
                .onLeft { snackBar.show(it.userMessage(), SnackBar.Severity.ERROR) }
            loadMembers(team, snackBar)
        }
    }

    fun remove(member: Member, snackBar: SnackBar) {
        val team = team ?: return
        run(snackBar) {
            syncService.removeMember(team.id, member.permissionId)
                .onRight { snackBar.show("${member.email ?: "The member"} no longer has access. They keep a copy of the team") }
                .onLeft { snackBar.show(it.userMessage(), SnackBar.Severity.ERROR) }
            loadMembers(team, snackBar)
        }
    }

    fun stopSharing(snackBar: SnackBar) {
        val team = team ?: return
        run(snackBar) {
            syncService.stopSharing(team.id)
                .onRight { snackBar.show("${team.name} is private again. The members keep a copy of it") }
                .onLeft { snackBar.show(it.userMessage(), SnackBar.Severity.ERROR) }
            withContext(Dispatchers.Main) { close() }
        }
    }

    private fun loadMembers(team: TeamlyticsPreview, snackBar: SnackBar?) {
        // a private team has no members yet
        if (team.id !in syncService.sharing.value) return
        scope.launch {
            syncService.members(team.id)
                .onRight { members -> withContext(Dispatchers.Main) { this@ShareTeamState.members = members } }
                .onLeft { error -> snackBar?.show(error.userMessage(), SnackBar.Severity.ERROR) }
        }
    }

    private fun run(snackBar: SnackBar, block: suspend () -> Unit) {
        if (isLoading) return
        isLoading = true
        scope.launch {
            try {
                block()
            } catch (e: Exception) {
                snackBar.show("Error: ${e.message}", SnackBar.Severity.ERROR)
            } finally {
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }
}
