package com.tambapps.pokemon.alakastats.ui.screen.editteam

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.navigator.Navigator
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.UserName
import com.tambapps.pokemon.alakastats.domain.usecase.EditTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.ui.SnackBar
import com.tambapps.pokemon.alakastats.ui.viewmodels.PokepasteEditingViewModel
import com.tambapps.pokemon.alakastats.util.isSdNameValid
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch

class EditTeamViewModel(
    pokepasteParser: PokepasteParser,
    private val editTeamlyticsUseCase: EditTeamlyticsUseCase,
    httpClient: HttpClient
) : PokepasteEditingViewModel(pokepasteParser, httpClient), ScreenModel {

    private var editingTeam: Teamlytics? = null
    val isEditing get() = editingTeam != null

    var teamName by mutableStateOf("")
        private set

    val sdNames = mutableStateListOf<String>()

    var showNewSdNameDialog by mutableStateOf(false)
        private set
    var newSdNameInput by mutableStateOf("")
        private set

    val isFormValid: Boolean
        get() = teamName.isNotBlank() && pokepaste.isNotBlank() && pokepasteError == null
                && sdNames.isNotEmpty()

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

    fun saveTeam(navigator: Navigator, snackBar: SnackBar) {
        if (isFormValid) {
            val pokepaste = pokepasteParser.tryParse(pokepaste) ?: return
            scope.launch {
                if (editingTeam != null) {
                    editTeamlyticsUseCase.edit(
                        editingTeam!!,
                        name = teamName,
                        sdNames = sdNames.map(::UserName),
                        pokePaste = pokepaste
                    ).getOrNull()
                } else {
                    editTeamlyticsUseCase.create(
                        name = teamName,
                        sdNames = sdNames.map(::UserName),
                        pokePaste = pokepaste
                    ).getOrNull()
                }
                navigator.pop()
                snackBar.show(
                    if (isEditing) "Updated team successfully"
                    else "Created team successfully", SnackBar.Severity.SUCCESS
                )
            }
        }
    }

    fun prepareEdition(teamlytics: Teamlytics) {
        editingTeam = teamlytics
        teamName = teamlytics.name
        pokepaste = teamlytics.pokePaste.toPokePasteString()
        validPokepaste()
        sdNames.clear()
        sdNames.addAll(teamlytics.sdNames.map { it.value })
    }
}