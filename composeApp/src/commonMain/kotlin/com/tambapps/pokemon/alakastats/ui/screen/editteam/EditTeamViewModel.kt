package com.tambapps.pokemon.alakastats.ui.screen.editteam

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.alakastats.domain.error.GetTeamlyticsError
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.UserName
import com.tambapps.pokemon.alakastats.domain.usecase.EditTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.infrastructure.sync.TeamSyncService
import com.tambapps.pokemon.alakastats.ui.SnackBar
import com.tambapps.pokemon.alakastats.ui.viewmodels.PokepasteEditingViewModel
import com.tambapps.pokemon.alakastats.util.isSdNameValid
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

class EditTeamViewModel(
    pokepasteParser: PokepasteParser,
    private val editTeamlyticsUseCase: EditTeamlyticsUseCase,
    httpClient: HttpClient,
    private val syncService: TeamSyncService,
) : PokepasteEditingViewModel(pokepasteParser, httpClient) {

    private var editingTeam: Teamlytics? = null
    val isEditing get() = editingTeam != null

    /** a new team can be kept on this device only, when the cloud is connected. Later, from the team's menu */
    val canChooseSync get() = !isEditing && syncService.isConnected
    var syncWithCloud by mutableStateOf(true)

    var teamName by mutableStateOf("")
        private set
    var format by mutableStateOf(Format.NONE)

    val sdNames = mutableStateListOf<String>()

    var showNewSdNameDialog by mutableStateOf(false)
        private set
    var newSdNameInput by mutableStateOf("")
        private set

    /** What the user entered, to tell whether they changed it. The paste as typed, even if not applied yet */
    private fun formState() = listOf(teamName, format, sdNames.toList(), pokepaste)

    // the empty form, or the team being edited once loaded
    private var initialFormState = formState()

    /** Whether leaving would lose what the user entered */
    val hasChanges get() = formState() != initialFormState

    val isFormValid: Boolean
        get() = teamName.isNotBlank() && appliedPokepaste != null && sdNames.isNotEmpty()

    fun updateTeamName(name: String) {
        teamName = name
    }

    fun showAddNameDialog() {
        showNewSdNameDialog = true
        newSdNameInput = ""
    }

    fun hideAddNameDialog() {
        showNewSdNameDialog = false
        newSdNameInput = ""
    }

    fun updateNewNameInput(input: String) {
        newSdNameInput = input
    }

    fun addShowdownName() {
        if (isSdNameValid(newSdNameInput) && !sdNames.contains(newSdNameInput)) {
            sdNames.add(newSdNameInput.trim())
            hideAddNameDialog()
        }
    }

    fun removeShowdownName(name: String) {
        sdNames.remove(name)
    }

    /** @param onError e.g. the team is shared with the user as a viewer: the screen stays, with what was entered */
    fun saveTeam(onSuccess: () -> Unit, onError: (GetTeamlyticsError) -> Unit) {
        if (isFormValid) {
            val pokepaste = appliedPokepaste ?: return
            scope.launch {
                val result = if (editingTeam != null) {
                    editTeamlyticsUseCase.edit(
                        editingTeam!!,
                        name = teamName,
                        sdNames = sdNames.map(::UserName),
                        pokePaste = pokepaste,
                        format = format
                    )
                } else {
                    editTeamlyticsUseCase.create(
                        name = teamName,
                        sdNames = sdNames.map(::UserName),
                        pokePaste = pokepaste,
                        format = format,
                        localOnly = canChooseSync && !syncWithCloud,
                    )
                }
                // navigating and showing a message from the main thread
                withContext(Dispatchers.Main) {
                    result.fold(onError, { onSuccess() })
                }
            }
        }
    }

    fun prepareEdition(teamlyticsId: Uuid, onFailure: () -> Unit) {
        scope.launch {
            val either = editTeamlyticsUseCase.get(teamlyticsId)
            withContext(Dispatchers.Main) {
                val teamlytics = either.getOrNull()
                // a viewer of a shared team has no way here but its URL, and couldn't save it
                if (teamlytics == null || syncService.isReadOnly(teamlyticsId)) {
                    onFailure.invoke()
                    return@withContext
                }
                editingTeam = teamlytics
                teamName = teamlytics.name
                loadPokepaste(teamlytics.pokePaste)
                format = teamlytics.format
                sdNames.clear()
                sdNames.addAll(teamlytics.sdNames.map { it.value })
                initialFormState = formState()
            }
        }
    }
}