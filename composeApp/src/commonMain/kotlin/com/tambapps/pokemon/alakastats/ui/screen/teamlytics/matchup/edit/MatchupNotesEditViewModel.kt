package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.matchup.edit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.GamePlan
import com.tambapps.pokemon.alakastats.domain.model.MatchupNotes
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.ui.SnackBar
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.viewmodels.PokepasteEditingViewModel
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

class MatchupNotesEditViewModel(
    pokepasteParser: PokepasteParser,
    httpClient: HttpClient,
    private val useCase: ManageTeamlyticsUseCase,
    val pokemonImageService: PokemonImageService,
    ) : PokepasteEditingViewModel(pokepasteParser, httpClient), ScreenModel {

    val isFormValid: Boolean get() = name.isNotBlank() && (gamePlanStates.isNotEmpty() && gamePlanStates.all { it.isValid })

    var name by mutableStateOf("")
        private set
    val gamePlanStates = mutableStateListOf<GamePlanState>()

    var compositionDialogFor by mutableStateOf<GamePlanState?>(null)

    fun generateMatchupNotes() = MatchupNotes(
        id = Uuid.random(),
        name = name,
        pokePaste = pokepasteParser.tryParse(pokepaste),
        gamePlans = gamePlanStates.map { it.toGamePlan() }
    )

    fun prepareEdition(matchupNotes: MatchupNotes) {
        name = matchupNotes.name
        pokepaste = matchupNotes.pokePaste?.toPokePasteString() ?: ""
        validPokepaste()
        gamePlanStates.clear()
        gamePlanStates.addAll(matchupNotes.gamePlans.map(GamePlanState::from))
    }

    fun updateName(name: String) {
        this.name = name
    }

    fun createGamePlan() {
        gamePlanStates.add(GamePlanState())
    }

    fun saveMatchupNotes(teamlytics: Teamlytics, original: MatchupNotes?, snackBar: SnackBar, onSuccess: () -> Unit) {
        scope.launch {
            val matchupNotes = generateMatchupNotes()
            val matchupNotesList =
                if (original != null) teamlytics.matchupNotes.map { if (it == original) matchupNotes else it }
                else teamlytics.matchupNotes + matchupNotes

            val either = useCase.save(teamlytics.copy(matchupNotes = matchupNotesList))

            withContext(Dispatchers.Main) {
                either.fold(
                    ifLeft = {
                        snackBar.show("Error: ${it.message}", SnackBar.Severity.ERROR)
                    },
                    ifRight = {
                        onSuccess()
                    }
                )
            }
        }
    }
}

class GamePlanState {
    var description by mutableStateOf("")
        private set

    var composition by mutableStateOf(listOf<PokemonName>())
        private set

    val isValid get() = description.isNotBlank() && composition.isNotEmpty()

    fun updateDescription(description: String) {
        this.description = description
    }

    fun updateComposition(composition: List<PokemonName>) {
        this.composition = composition
    }

    fun toGamePlan() = GamePlan(description = description, composition = composition)
    companion object {
        fun from(gamePlan: GamePlan) = GamePlanState().apply {
            description = gamePlan.description
            gamePlan.composition?.let { composition = it }
        }
    }
}