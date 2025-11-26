package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.matchup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.alakastats.domain.model.MatchupNotes
import com.tambapps.pokemon.alakastats.domain.usecase.ManageMatchupNotesUseCase
import com.tambapps.pokemon.alakastats.ui.SnackBar
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.TeamlyticsTabViewModel
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MatchupNotesViewModel(
    override val useCase: ManageMatchupNotesUseCase,
    val pokemonImageService: PokemonImageService,
): TeamlyticsTabViewModel() {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun addMatchupNotes(snackBar: SnackBar, notes: MatchupNotes) {
        scope.launch {
            val either = useCase.addMatchupNotes(notes)
            withContext(Dispatchers.Main) {
                either.fold(
                    ifLeft = {
                        snackBar.show("Error: ${it.message}", SnackBar.Severity.ERROR)
                    },
                    ifRight = {
                        snackBar.show("Successfully created Matchup", SnackBar.Severity.SUCCESS)
                    }
                )
            }
        }
    }

    override var isTabLoading by mutableStateOf(false)
        private set

    val team get() = useCase.originalTeam
    val matchupNotes get() = team.matchupNotes
    val hasMatchupNotes get() = matchupNotes.isNotEmpty()
}