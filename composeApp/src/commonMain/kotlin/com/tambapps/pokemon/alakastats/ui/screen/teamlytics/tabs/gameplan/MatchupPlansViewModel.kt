package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.gameplan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.domain.model.MatchupPlan
import com.tambapps.pokemon.alakastats.domain.usecase.ManageMatchupPlansUseCase
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.TeamlyticsTabViewModel
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class EditMatchupMode

object NoEdit: EditMatchupMode()
class EditMatchup(val matchupPlan: MatchupPlan): EditMatchupMode()
object CreateMatchup: EditMatchupMode()

class MatchupPlansViewModel(
    override val useCase: ManageMatchupPlansUseCase,
    override val pokemonImageService: PokemonImageService,
): TeamlyticsTabViewModel() {

    override val isTabLoading = false
    val team get() = useCase.originalTeam
    val matchupPlans get() = team.matchupPlans
    val hasMatchupPlans get() = matchupPlans.isNotEmpty()

    var editMatchupMode by mutableStateOf<EditMatchupMode>(NoEdit)

    private val scope = CoroutineScope(Dispatchers.Default)

    fun saveMatchup(matchupPlan: MatchupPlan, onSuccess: () -> Unit, onError: (DomainError) -> Unit) {
        val matchupPlansList = when(val mode = editMatchupMode) {
            NoEdit -> return
            CreateMatchup -> team.matchupPlans + matchupPlan
            is EditMatchup -> team.matchupPlans.map { if (it == mode.matchupPlan) matchupPlan else it }
        }

        scope.launch {
            val either = useCase.setMatchupPlans(matchupPlansList)
            withContext(Dispatchers.Main) {
                either.fold(onError, { onSuccess.invoke(); editMatchupMode = NoEdit })
            }
        }
    }

    fun deleteMatchup(matchupPlan: MatchupPlan, onSuccess: () -> Unit, onError: (DomainError) -> Unit) {
        scope.launch {
            // don't care about the result. I am la
            val either = useCase.setMatchupPlans(team.matchupPlans.filter { it.id != matchupPlan.id })
            withContext(Dispatchers.Main) {
                either.fold(onError, { onSuccess.invoke(); editMatchupMode = NoEdit })
            }
        }
    }
}