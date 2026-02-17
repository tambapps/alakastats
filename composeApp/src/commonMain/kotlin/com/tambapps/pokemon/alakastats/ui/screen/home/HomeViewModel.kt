package com.tambapps.pokemon.alakastats.ui.screen.home

import alakastats.composeapp.generated.resources.Res
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.navigator.Navigator
import com.tambapps.pokemon.alakastats.domain.error.StorageError
import com.tambapps.pokemon.alakastats.domain.error.TeamlyticsNotFound
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.domain.model.withComputedElo
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamlyticsListUseCase
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.downloadToFile
import com.tambapps.pokemon.alakastats.infrastructure.service.ReplayAnalyticsService
import com.tambapps.pokemon.alakastats.ui.SnackBar
import com.tambapps.pokemon.alakastats.ui.screen.editteam.EditTeamScreen
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.TeamlyticsScreen
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

class HomeViewModel(
    val imageService: PokemonImageService,
    private val replayAnalyticsService: ReplayAnalyticsService,
    private val useCase: ManageTeamlyticsListUseCase
): ScreenModel {
    
    val teamlyticsList: SnapshotStateList<TeamlyticsPreview> = mutableStateListOf()
    private val scope = CoroutineScope(Dispatchers.Default)
    
    var expandedMenuTeamId by mutableStateOf<Uuid?>(null)
        private set
    
    var teamToDelete by mutableStateOf<TeamlyticsPreview?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var samplesToShow by mutableStateOf<List<TeamlyticsPreview>?>(null)
        private set

    var teamToImport by mutableStateOf<Teamlytics?>(null)
        private set

    fun loadTeams() {
        scope.launch { doLoadTeams() }
    }

    fun importTeam(snackBar: SnackBar) {
        scope.launch {
            val file = FileKit.openFilePicker(
                title = "Pick Alakastats or PokeShowStats team",
                type = FileKitType.File("json"),
            )
            if (file == null) {
                return@launch
            }
            importTeam(snackBar, file.readBytes())
        }
    }

    private suspend fun importTeam(snackBar: SnackBar, saveBytes: ByteArray, importDirectly: Boolean = false) {
        withContext(Dispatchers.Main) {
            isLoading = true
        }
        val eitherTeam = useCase.loadTeam(saveBytes)
        withContext(Dispatchers.Main) {
            eitherTeam.fold(
                ifLeft = {
                    isLoading = false
                    snackBar.show("Couldn't import team: ${it.message}", SnackBar.Severity.ERROR)
                },
                ifRight = {
                    if (importDirectly) {
                        doImport(snackBar, it)
                    } else {
                        isLoading = false
                        // will open confirmation dialog
                        teamToImport = it
                    }
                }
            )
        }
    }

    fun confirmImport(snackBar: SnackBar) = teamToImport?.let { doImport(snackBar, it) } ?: Unit

    private fun doImport(snackBar: SnackBar, team: Teamlytics) {
        isLoading = true
        scope.launch {
            val either = useCase.create(team)
            val previews = useCase.list()
            withContext(Dispatchers.Main) {
                either.fold(ifLeft = {
                    snackBar.show("Error: ${it.message}", SnackBar.Severity.ERROR)
                }, ifRight = {

                })
                teamlyticsList.clear()
                teamlyticsList.addAll(previews.sortedWith(compareBy({ - it.lastUpdatedAt.epochSeconds }, { it.name })))
                isLoading = false
                teamToImport = null
            }
        }
    }

    fun dismissImportTeamDialog() {
        teamToImport = null
    }

    fun showSamplesDialog() {
        samplesToShow = useCase.listSamplePreviews()
    }

    fun hideSamplesDialog() {
        samplesToShow = null
    }

    fun importSample(snackBar: SnackBar, preview: TeamlyticsPreview) {
        val fileName = preview.name.lowercase().replace(' ', '_')
        scope.launch {
            val saveBytes = Either.catch { Res.readBytes("files/samples/$fileName.json") }
                .getOrNull() ?: return@launch
            importTeam(snackBar, saveBytes, importDirectly = true)
        }
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

    fun reloadReplays(preview: TeamlyticsPreview, snackBar: SnackBar) {
        isLoading = true
        scope.launch {
            val either = either {
                val team = useCase.get(preview.id).bind()
                val reloadedReplays = team.replays
                    .distinctBy { it.reference } // just in case there were some duplicates
                    .map { replay ->
                        async {
                            replay.url?.let { replayUrl ->
                                replayAnalyticsService.fetch(replayUrl).getOrElse { error ->
                                    withContext(Dispatchers.Main) {
                                        snackBar.show("Failed to fetch replay: ${error.message}", SnackBar.Severity.ERROR)
                                    }
                                    null
                                }
                            }?.completedWith(replay) ?: replay
                        }
                    }
                    .awaitAll()
                useCase.save(team.copy(replays = reloadedReplays.withComputedElo())).bind()
                doLoadTeams()
            }
            withContext(Dispatchers.Main) {
                isLoading = false
                either.fold(
                    ifLeft = {
                        snackBar.show("Failed to reload replays: ${it.message}", SnackBar.Severity.ERROR)
                    },
                    ifRight = {
                        snackBar.show("Successfully reloaded replays")
                    }
                )
            }
        }
        hideMenu()
    }

    fun exportTeam(preview: TeamlyticsPreview, snackBar: SnackBar) {
        if (isLoading) {
            return
        }
        isLoading = true
        scope.launch {
            useCase.get(preview.id).fold(
                ifLeft = {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        snackBar.show("Error: Couldn't retrieve team", SnackBar.Severity.ERROR)
                    }
                },
                ifRight = { team ->
                    val success = downloadToFile(team.name, "json", useCase.export(team))

                    withContext(Dispatchers.Main) {
                        isLoading = false
                        if (success) {
                            snackBar.show("Successfully exported team", SnackBar.Severity.SUCCESS)
                        }
                    }
                }
            )
        }
        hideMenu()
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
                        navigator.push(EditTeamScreen(fullTeam.id))
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