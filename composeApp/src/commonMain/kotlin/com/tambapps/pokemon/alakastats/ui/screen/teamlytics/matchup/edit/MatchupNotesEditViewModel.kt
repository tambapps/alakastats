package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.matchup.edit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import com.tambapps.pokemon.alakastats.domain.model.MatchupNotes
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser

class MatchupNotesEditViewModel(
    private val pokepasteParser: PokepasteParser,
    ) : ScreenModel {

    var name by mutableStateOf("")
        private set

    var pokepaste by mutableStateOf("")
        private set



    fun prepareEdition(matchupNotes: MatchupNotes) {
        name = matchupNotes.name
        pokepaste = matchupNotes.pokePaste?.toPokePasteString() ?: ""
        //validPokepaste()
        // TODO do other fields
    }

}