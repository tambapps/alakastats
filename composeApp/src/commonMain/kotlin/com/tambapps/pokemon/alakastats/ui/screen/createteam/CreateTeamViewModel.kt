package com.tambapps.pokemon.alakastats.ui.screen.createteam

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.navigator.Navigator
import com.tambapps.pokemon.pokepaste.parser.PokePaste
import com.tambapps.pokemon.pokepaste.parser.PokePasteParseException
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser

class CreateTeamViewModel(
    private val pokepasteParser: PokepasteParser
) : ScreenModel {

    var teamName by mutableStateOf("")
        private set
    
    var pokepaste by mutableStateOf("")
        private set
    var pokepasteError by mutableStateOf<String?>(null)
        private set
    
    val showdownNames = mutableStateListOf<String>()
    
    var isShowAddNameDialog by mutableStateOf(false)
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
    
    fun openAddNameDialog() {
        isShowAddNameDialog = true
        newNameInput = ""
    }
    
    fun hideAddNameDialog() {
        isShowAddNameDialog = false
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
            // TODO: Process the team creation with pokepaste parsing and showdown names
            // For now, just navigate back
            navigator.pop()
        }
    }
}