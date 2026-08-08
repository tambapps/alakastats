package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.navigator.Navigator
import com.tambapps.pokemon.MoveName
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

private val TAILWIND_MOVE = MoveName("tailwind")

class SpeedInteractionsTeamSetupViewModel(
    private val teamId: Uuid,
    private val teamlyticsRepository: TeamlyticsRepository
) : ScreenModel {

    private val scope = CoroutineScope(Dispatchers.Default)

    var isLoading by mutableStateOf(true)
        private set
    var hasTailwindUser by mutableStateOf(false)
        private set

    var allowOpponentScarf by mutableStateOf(false)
        private set
    var allowParalysis by mutableStateOf(false)
        private set
    var allowTailwind by mutableStateOf(false)
        private set
    var includeNonMegaBaseForms by mutableStateOf(false)
        private set
    var allowReducingNature by mutableStateOf(false)
        private set

    init {
        scope.launch {
            teamlyticsRepository.get(teamId).fold(
                ifLeft = {},
                ifRight = { team ->
                    hasTailwindUser = team.pokePaste.pokemons.any { pokemon ->
                        pokemon.moves.any { it.matches(TAILWIND_MOVE) }
                    }
                }
            )
            isLoading = false
        }
    }

    fun toggleOpponentScarf() {
        allowOpponentScarf = !allowOpponentScarf
    }

    fun toggleParalysis() {
        allowParalysis = !allowParalysis
    }

    fun toggleTailwind() {
        allowTailwind = !allowTailwind
    }

    fun toggleNonMegaBaseForms() {
        includeNonMegaBaseForms = !includeNonMegaBaseForms
    }

    fun toggleReducingNature() {
        allowReducingNature = !allowReducingNature
    }

    fun startQuiz(navigator: Navigator) {
        navigator.push(
            SpeedInteractionsQuizScreen(
                SpeedInteractionsMode.Team(
                    teamId = teamId,
                    allowOpponentScarf = allowOpponentScarf,
                    allowParalysis = allowParalysis,
                    allowTailwind = allowTailwind && hasTailwindUser,
                    includeNonMegaBaseForms = includeNonMegaBaseForms,
                    allowReducingNature = allowReducingNature
                )
            )
        )
    }
}
