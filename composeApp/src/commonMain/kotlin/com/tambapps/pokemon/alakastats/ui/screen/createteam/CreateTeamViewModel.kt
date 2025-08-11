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
    
    val showdownNames = mutableStateListOf<String>()
    
    var showAddNameDialog by mutableStateOf(false)
        private set
    var newNameInput by mutableStateOf("")
        private set
    
    val isFormValid: Boolean
        get() = teamName.isNotBlank() && pokepaste.isNotBlank() && pokepasteError == null
                && showdownNames.isNotEmpty()
    
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
        get() = newNameInput.isNotBlank() && 
                !newNameInput.contains('/') && 
                newNameInput.length <= 30
    
    fun showAddNameDialog() {
        showAddNameDialog = true
        newNameInput = ""
    }
    
    fun hideAddNameDialog() {
        showAddNameDialog = false
        newNameInput = ""
    }
    
    fun updateNewNameInput(input: String) {
        newNameInput = input
    }
    
    fun addShowdownName() {
        if (isNewNameValid && !showdownNames.contains(newNameInput)) {
            showdownNames.add(newNameInput)
            hideAddNameDialog()
        }
    }
    
    fun removeShowdownName(name: String) {
        showdownNames.remove(name)
    }

    fun createTeam(navigator: Navigator) {
        if (isFormValid) {
            val pokepaste = pokepasteParser.tryParse(pokepaste) ?: return
            scope.launch {
                createTeamlyticsUseCase.create(
                    name = teamName,
                    sdNames = showdownNames,
                    pokePaste = pokepaste
                )
                navigator.pop()
            }
        }
    }
}