package com.tambapps.pokemon.alakastats.ui.screen.createteam

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.navigator.Navigator
import com.tambapps.pokemon.alakastats.domain.usecase.CreateTeamlyticsUseCase
import com.tambapps.pokemon.pokepaste.parser.PokePaste
import com.tambapps.pokemon.pokepaste.parser.PokePasteParseException
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CreateTeamViewModel(
    private val pokepasteParser: PokepasteParser,
    private val createTeamlyticsUseCase: CreateTeamlyticsUseCase
) : ScreenModel {

    private val scope = CoroutineScope(Dispatchers.Default)

    var teamName by mutableStateOf("")
        private set
    
    var pokepaste by mutableStateOf("")
        private set
    var pokepasteError by mutableStateOf<String?>(null)
        private set
    
    val sdNames = mutableStateListOf<String>()
    
    var showNewSdNameDialog by mutableStateOf(false)
        private set
    var newSdNameInput by mutableStateOf("")
        private set
    
    var showPokePasteUrlDialog by mutableStateOf(false)
        private set
    var pokePasteUrlInput by mutableStateOf("")
        private set
    
    val isFormValid: Boolean
        get() = teamName.isNotBlank() && pokepaste.isNotBlank() && pokepasteError == null
                && sdNames.isNotEmpty()
    
    fun updateTeamName(name: String) {
        teamName = name
    }
    
    fun updatePokepaste(paste: String) {
        pokepaste = paste
        validPokepaste()
    }

    private fun validPokepaste(): PokePaste? {
        if (pokepaste.isBlank()) return null
        try {
            val p = pokepasteParser.parse(pokepaste)
            pokepasteError = null
            return p
        } catch (e: PokePasteParseException) {
            pokepasteError = e.message
            return null
        }
    }

    val isNewNameValid: Boolean
        get() = newSdNameInput.isNotBlank() &&
                !newSdNameInput.contains('/') &&
                newSdNameInput.length <= 30
    
    val isUrlValid: Boolean
        get() = isValidUrl(pokePasteUrlInput)
    
    private fun isValidUrl(url: String): Boolean {
        if (url.isBlank()) return false
        return try {
            val trimmedUrl = url.trim()
            trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://")
        } catch (e: Exception) {
            false
        }
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
        if (isNewNameValid && !sdNames.contains(newSdNameInput)) {
            sdNames.add(newSdNameInput)
            hideAddNameDialog()
        }
    }
    
    fun removeShowdownName(name: String) {
        sdNames.remove(name)
    }
    
    fun showUrlDialog() {
        showPokePasteUrlDialog = true
        pokePasteUrlInput = ""
    }
    
    fun hideUrlDialog() {
        showPokePasteUrlDialog = false
        pokePasteUrlInput = ""
    }
    
    fun updateUrlInput(input: String) {
        pokePasteUrlInput = input
    }
    
    fun loadFromUrl() {
        if (isUrlValid) {
            // For now, just close the dialog as requested
            // TODO: Implement actual URL loading functionality
            hideUrlDialog()
        }
    }

    fun createTeam(navigator: Navigator) {
        if (isFormValid) {
            val pokepaste = pokepasteParser.tryParse(pokepaste) ?: return
            scope.launch {
                createTeamlyticsUseCase.create(
                    name = teamName,
                    sdNames = sdNames,
                    pokePaste = pokepaste
                )
                navigator.pop()
            }
        }
    }
}