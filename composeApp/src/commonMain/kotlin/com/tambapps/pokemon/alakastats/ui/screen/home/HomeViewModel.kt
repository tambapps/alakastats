package com.tambapps.pokemon.alakastats.ui.screen.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.navigator.Navigator
import com.tambapps.pokemon.alakastats.domain.error.StorageError
import com.tambapps.pokemon.alakastats.domain.error.TeamlyticsNotFound
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamlyticsListUseCase
import com.tambapps.pokemon.alakastats.ui.SnackBar
import com.tambapps.pokemon.alakastats.ui.screen.editteam.EditTeamScreen
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.TeamlyticsScreen
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

class HomeViewModel(
    val imageService: PokemonImageService,
    private val useCase: ManageTeamlyticsListUseCase
): ScreenModel {
    
    val teamlyticsList: SnapshotStateList<TeamlyticsPreview> = mutableStateListOf()
    private val scope = CoroutineScope(Dispatchers.Default)
    
    var expandedMenuTeamId by mutableStateOf<Uuid?>(null)
        private set
    
    var teamToDelete by mutableStateOf<TeamlyticsPreview?>(null)
        private set
    

    fun loadTeams() {
        scope.launch { doLoadTeams() }
    }

    private suspend fun doLoadTeams() {
        val previews = useCase.list()
        withContext(Dispatchers.Main) {
            teamlyticsList.clear()
            teamlyticsList.addAll(previews.sortedWith(compareBy({ - it.lastUpdatedAt.epochSeconds }, { it.name })))
        }
    }

    fun showMenu(teamId: Uuid) {
        expandedMenuTeamId = teamId
    }
    
    fun hideMenu() {
        expandedMenuTeamId = null
    }

    fun consultTeam(team: TeamlyticsPreview, navigator: Navigator) {
        navigator.push(TeamlyticsScreen(team.id))
    }

    fun editTeam(team: TeamlyticsPreview, navigator: Navigator, snackBar: SnackBar) {
        scope.launch {
            useCase.get(team.id).fold(
                ifLeft =  { error ->
                    when(error) {
                        is StorageError -> snackBar.show("Storage error: ${error.message}", SnackBar.Severity.ERROR)
                        is TeamlyticsNotFound -> snackBar.show("The team could not be found", SnackBar.Severity.ERROR)
                    }
                },
                ifRight = { fullTeam ->
                    withContext(Dispatchers.Main) {
                        navigator.push(EditTeamScreen(fullTeam))
                    }
                }
            )
        }
        hideMenu()
    }
    
    fun deleteTeamDialog(team: TeamlyticsPreview) {
        teamToDelete = team
        hideMenu()
    }
    
    fun dismissDeleteDialog() {
        teamToDelete = null
    }
    
    fun confirmDelete() {
        teamToDelete?.let { team ->
            scope.launch {
                useCase.delete(team.id)
                teamToDelete = null
                doLoadTeams()
            }
        }
    }
}