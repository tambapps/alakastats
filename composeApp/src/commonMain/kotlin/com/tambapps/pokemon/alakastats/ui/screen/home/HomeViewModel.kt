package com.tambapps.pokemon.alakastats.ui.screen.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.navigator.Navigator
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.domain.usecase.TeamlyticsHomeUseCase
import com.tambapps.pokemon.alakastats.ui.screen.editteam.EditTeamScreen
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.TeamDetailsScreen
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

class HomeViewModel(
    val imageService: PokemonImageService,
    private val useCase: TeamlyticsHomeUseCase
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
            teamlyticsList.addAll(previews)
        }
    }

    fun showMenu(teamId: Uuid) {
        expandedMenuTeamId = teamId
    }
    
    fun hideMenu() {
        expandedMenuTeamId = null
    }

    fun consultTeam(team: TeamlyticsPreview, navigator: Navigator) {
        scope.launch {
            val fullTeam = useCase.get(team.id) ?: return@launch
            withContext(Dispatchers.Main) {
                navigator.push(TeamDetailsScreen(fullTeam))
            }
        }

    }
    fun editTeam(team: TeamlyticsPreview, navigator: Navigator) {
        scope.launch {
            val fullTeam = useCase.get(team.id) ?: return@launch
            withContext(Dispatchers.Main) {
                navigator.push(EditTeamScreen(fullTeam))
            }
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