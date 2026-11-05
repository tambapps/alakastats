package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.gameplan.edit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.GamePlan
import com.tambapps.pokemon.alakastats.domain.model.MatchupPlan
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.ui.viewmodels.PokepasteEditingViewModel
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import io.ktor.client.HttpClient
import kotlin.uuid.Uuid

class MatchupPlanEditViewModel(
    pokepasteParser: PokepasteParser,
    httpClient: HttpClient,
) : PokepasteEditingViewModel(pokepasteParser, httpClient) {

    /**
     * Why the matchup can't be saved yet, or null if it can
     */
    val invalidReason: String? get() {
        if (name.isBlank()) return "Name the matchup"
        if (gamePlanStates.isEmpty()) return "Add at least one game plan"
        val invalidIndex = gamePlanStates.indexOfFirst { !it.isValid }
        if (invalidIndex >= 0) return "Game plan ${invalidIndex + 1} is empty: choose its Pokémon or describe it"
        return null
    }

    val isFormValid: Boolean get() = invalidReason == null

    var name by mutableStateOf("")
        private set
    val gamePlanStates = mutableStateListOf<GamePlanState>()

    var compositionDialogFor by mutableStateOf<GamePlanState?>(null)
    var addReplayDialogFor by mutableStateOf<GamePlanState?>(null)
    var gamePlanToDelete by mutableStateOf<GamePlanState?>(null)

    // what the form held when opened, to know whether leaving it loses changes
    private var original: MatchupPlan? = null

    val hasChanges: Boolean get() = original?.let { generateMatchupPlan(it).normalized() != it.normalized() } ?: false

    fun generateMatchupPlan(matchupPlan: MatchupPlan?) = MatchupPlan(
        id = matchupPlan?.id ?: Uuid.random(),
        name = name,
        // the opponent team
        pokePaste = appliedPokepaste,
        gamePlans = gamePlanStates.map { it.toGamePlan() }
    )

    fun prepareCreation() {
        if (original != null) return
        createGamePlan()
        original = generateMatchupPlan(null)
    }

    fun prepareEdition(matchupPlan: MatchupPlan) {
        name = matchupPlan.name
        loadPokepaste(matchupPlan.pokePaste)
        gamePlanStates.clear()
        gamePlanStates.addAll(matchupPlan.gamePlans.map { GamePlanState.from(it) })
        original = matchupPlan
    }

    fun updateName(name: String) {
        this.name = name
    }

    fun createGamePlan() {
        gamePlanStates.add(GamePlanState())
    }

    fun deleteGamePlan(gamePlanState: GamePlanState) {
        gamePlanStates.remove(gamePlanState)
        if (gamePlanToDelete == gamePlanState) gamePlanToDelete = null
    }

    /**
     * Moves the game plan by the given number of places (negative is up). Does nothing if it would leave the list
     */
    fun moveGamePlan(gamePlanState: GamePlanState, offset: Int) {
        val from = gamePlanStates.indexOf(gamePlanState)
        val to = from + offset
        if (from < 0 || to !in gamePlanStates.indices) return
        gamePlanStates.add(to, gamePlanStates.removeAt(from))
    }
}

// a game plan without composition is stored as null or as an empty list: both are the same plan
private fun MatchupPlan.normalized() = copy(gamePlans = gamePlans.map { it.copy(composition = it.composition.orEmpty()) })

class GamePlanState() {
    // only identifies the plan's card in the list, e.g. while it moves
    val key = Uuid.random().toString()

    var description by mutableStateOf("")
        private set

    var composition by mutableStateOf(listOf<PokemonName>())
        private set

    var exampleReplays by mutableStateOf(emptyList<ReplayAnalytics>())
        private set

    val isValid get() = description.isNotBlank() || composition.isNotEmpty()

    fun updateDescription(description: String) {
        this.description = description
    }

    fun updateComposition(composition: List<PokemonName>) {
        this.composition = composition
    }

    fun updateExampleReplays(exampleReplays: List<ReplayAnalytics>) {
        this.exampleReplays = exampleReplays
    }

    fun toGamePlan() = GamePlan(description = description, composition = composition, exampleReplays = exampleReplays)

    companion object {
        fun from(gamePlan: GamePlan) = GamePlanState().apply {
            description = gamePlan.description
            gamePlan.composition?.let { composition = it }
            exampleReplays = gamePlan.exampleReplays
        }
    }
}
