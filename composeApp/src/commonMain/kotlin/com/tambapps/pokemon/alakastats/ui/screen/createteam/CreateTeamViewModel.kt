package com.tambapps.pokemon.alakastats.ui.screen.createteam

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.navigator.Navigator
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
    
    val isFormValid: Boolean
        get() = teamName.isNotBlank() && pokepaste.isNotBlank() && pokepasteError == null
    
    fun updateTeamName(name: String) {
        teamName = name
    }
    
    fun updatePokepaste(paste: String) {
        pokepaste = paste
        validPokepaste()
    }

    private fun validPokepaste() {
        if (pokepaste.isBlank()) return
        try {
            pokepasteParser.parse(pokepaste)
            pokepasteError = null
        } catch (e: PokePasteParseException) {
            pokepasteError = e.message
        }
    }

    fun createTeam(navigator: Navigator) {

        if (isFormValid) {
            // TODO: Process the team creation with pokepaste parsing
            // For now, just navigate back
            navigator.pop()
        }
    }
}