package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.matchup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.domain.model.MatchupNotes
import com.tambapps.pokemon.alakastats.domain.usecase.ManageMatchupNotesUseCase
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class EditMatchupMode

object NoEdit: EditMatchupMode()
class EditMatchup(val matchupNotes: MatchupNotes): EditMatchupMode()
object CreateMatchup: EditMatchupMode()

class MatchupNotesViewModel(
    val useCase: ManageMatchupNotesUseCase,
    val pokemonImageService: PokemonImageService,
) {

    val team get() = useCase.originalTeam
    val matchupNotes get() = team.matchupNotes
    val hasMatchupNotes get() = matchupNotes.isNotEmpty()

    var editMatchupMode by mutableStateOf<EditMatchupMode>(NoEdit)

    private val scope = CoroutineScope(Dispatchers.Default)

    fun saveMatchup(matchupNotes: MatchupNotes, onSuccess: () -> Unit, onError: (DomainError) -> Unit) {
        val matchupNotesList = when(val mode = editMatchupMode) {
            NoEdit -> return
            CreateMatchup -> team.matchupNotes + matchupNotes
            is EditMatchup -> team.matchupNotes.map { if (it == mode.matchupNotes) matchupNotes else it }
        }

        scope.launch {
            val either = useCase.setMatchupNotes(matchupNotesList)
            withContext(Dispatchers.Main) {
                either.fold(onError, { onSuccess.invoke(); editMatchupMode = NoEdit })
            }
        }
    }
}