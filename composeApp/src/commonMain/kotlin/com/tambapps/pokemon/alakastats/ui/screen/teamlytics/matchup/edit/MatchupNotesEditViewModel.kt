package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.matchup.edit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import com.tambapps.pokemon.alakastats.domain.model.MatchupNotes
import com.tambapps.pokemon.alakastats.ui.viewmodels.PokepasteEditingViewModel
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import io.ktor.client.HttpClient

class MatchupNotesEditViewModel(
    pokepasteParser: PokepasteParser,
    httpClient: HttpClient
    ) : PokepasteEditingViewModel(pokepasteParser, httpClient), ScreenModel {

    var name by mutableStateOf("")
        private set

    fun prepareEdition(matchupNotes: MatchupNotes) {
        name = matchupNotes.name
        pokepaste = matchupNotes.pokePaste?.toPokePasteString() ?: ""
        validPokepaste()
        // TODO do other fields
    }

    fun updateName(name: String) {
        this.name = name
    }
}