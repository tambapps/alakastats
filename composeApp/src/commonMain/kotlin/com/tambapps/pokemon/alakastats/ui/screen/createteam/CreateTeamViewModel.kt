package com.tambapps.pokemon.alakastats.ui.screen.createteam

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.navigator.Navigator
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.usecase.CreateTeamlyticsUseCase
import com.tambapps.pokemon.pokepaste.parser.PokePaste
import com.tambapps.pokemon.pokepaste.parser.PokePasteParseException
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.withContext

class CreateTeamViewModel(
    private val pokepasteParser: PokepasteParser,
    private val createTeamlyticsUseCase: CreateTeamlyticsUseCase,
    private val httpClient: HttpClient
) : ScreenModel {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var editingTeam: Teamlytics? = null

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
    
    var isLoadingUrl by mutableStateOf(false)
        private set
    var urlError by mutableStateOf<String?>(null)
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
        urlError = null
    }
    
    fun hideUrlDialog() {
        showPokePasteUrlDialog = false
        pokePasteUrlInput = ""
        urlError = null
        isLoadingUrl = false
    }
    
    fun updateUrlInput(input: String) {
        pokePasteUrlInput = input
    }
    
    fun loadFromUrl() {
        if (isUrlValid && !isLoadingUrl) {
            isLoadingUrl = true
            urlError = null
            
            scope.launch {
                try {
                    var url = pokePasteUrlInput
                    if (!url.endsWith("/raw")) {
                      if (!url.endsWith("/")) url = "$url/"
                        url += "raw"
                    }
                    val response = httpClient.get(url)
                    val content = response.bodyAsText()

                    withContext(Dispatchers.Main) {
                        updatePokepaste(content)
                        hideUrlDialog()
                    }
                } catch (e: Exception) {
                    urlError = "Failed to load URL: ${e.message}"
                } finally {
                    isLoadingUrl = false
                }
            }
        }
    }

    fun saveTeam(navigator: Navigator) {
        if (isFormValid) {
            val pokepaste = pokepasteParser.tryParse(pokepaste) ?: return
            scope.launch {
                if (editingTeam != null) {
                    createTeamlyticsUseCase.edit(editingTeam!!,
                        name = teamName,
                        sdNames = sdNames,
                        pokePaste = pokepaste)
                } else {
                    createTeamlyticsUseCase.create(
                        name = teamName,
                        sdNames = sdNames,
                        pokePaste = pokepaste
                    )
                }
                navigator.pop()
            }
        }
    }
    
    fun prepareEdition(teamlytics: Teamlytics) {
        editingTeam = teamlytics
        teamName = teamlytics.name
        pokepaste = teamlytics.pokePaste.toPokePasteString()
        validPokepaste()
        sdNames.clear()
        sdNames.addAll(teamlytics.sdNames)
    }
}