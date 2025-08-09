package com.tambapps.pokemon.alakastats.ui.screen.createteam

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.navigator.Navigator

class CreateTeamViewModel : ScreenModel {
    
    var teamName by mutableStateOf("")
        private set
    
    var pokepaste by mutableStateOf("")
        private set
    
    val isFormValid: Boolean
        get() = teamName.isNotBlank() && pokepaste.isNotBlank()
    
    fun updateTeamName(name: String) {
        teamName = name
    }
    
    fun updatePokepaste(paste: String) {
        pokepaste = paste
    }
    
    fun createTeam(navigator: Navigator) {
        if (isFormValid) {
            // TODO: Process the team creation with pokepaste parsing
            // For now, just navigate back
            navigator.pop()
        }
    }
}