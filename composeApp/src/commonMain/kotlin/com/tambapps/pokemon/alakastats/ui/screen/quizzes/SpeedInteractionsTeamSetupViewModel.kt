package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.tambapps.pokemon.MoveName
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.alakastats.ui.navigation.AppNavigator
import com.tambapps.pokemon.alakastats.ui.navigation.AppRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

private val TAILWIND_MOVE = MoveName("tailwind")

class SpeedInteractionsTeamSetupViewModel(
    private val teamId: Uuid,
    private val teamlyticsRepository: TeamlyticsRepository
) : ViewModel() {

    private val scope = CoroutineScope(Dispatchers.Default)

    var isLoading by mutableStateOf(true)
        private set
    var hasTailwindUser by mutableStateOf(false)
        private set
    var isTeamLoadFailed by mutableStateOf(false)
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
                ifLeft = { isTeamLoadFailed = true },
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

    fun startQuiz(navigator: AppNavigator) {
        navigator.push(
            AppRoute.SpeedInteractionsQuiz(
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
