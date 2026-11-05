package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tambapps.pokemon.alakastats.ui.navigation.LocalAppNavigator
import com.tambapps.pokemon.alakastats.ui.screen.home.buttonTextStyle
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import kotlin.uuid.Uuid
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SpeedInteractionsTeamSetupScreen(teamId: Uuid) {
    val viewModel = koinViewModel<SpeedInteractionsTeamSetupViewModel> { parametersOf(teamId) }
    val isCompact = LocalIsCompact.current

    if (isCompact) {
        SpeedInteractionsTeamSetupScreenMobile(viewModel)
    } else {
        SpeedInteractionsTeamSetupScreenDesktop(viewModel)
    }
}

@Composable
internal fun SpeedInteractionsTeamSetupOptions(viewModel: SpeedInteractionsTeamSetupViewModel) {
    QuizOptionCheckbox(
        label = "Choice Scarf",
        description = "The opponent could be holding a Choice Scarf (×1.5 Speed)",
        checked = viewModel.allowOpponentScarf,
        onCheckedChange = { viewModel.toggleOpponentScarf() }
    )

    QuizOptionCheckbox(
        label = "Paralysis",
        description = "Either side could be paralyzed (÷2 Speed)",
        checked = viewModel.allowParalysis,
        onCheckedChange = { viewModel.toggleParalysis() }
    )

    if (viewModel.hasTailwindUser) {
        QuizOptionCheckbox(
            label = "Tailwind",
            description = "Your Pokemon could be under Tailwind (×2 Speed)",
            checked = viewModel.allowTailwind,
            onCheckedChange = { viewModel.toggleTailwind() }
        )
    }

    QuizOptionCheckbox(
        label = "Include non-mega base forms",
        description = "Also quiz mega-capable Pokemon in their base form",
        checked = viewModel.includeNonMegaBaseForms,
        onCheckedChange = { viewModel.toggleNonMegaBaseForms() }
    )

    QuizOptionCheckbox(
        label = "Negative Speed natures",
        description = "The opponent's revealed nature could also be Speed-reducing (by default, only neutral or Speed-boosting are proposed)",
        checked = viewModel.allowReducingNature,
        onCheckedChange = { viewModel.toggleReducingNature() }
    )
}

@Composable
internal fun StartSpeedInteractionsTeamQuizButton(viewModel: SpeedInteractionsTeamSetupViewModel, modifier: Modifier = Modifier) {
    val navigator = LocalAppNavigator.current
    Button(
        onClick = { viewModel.startQuiz(navigator) },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
    ) {
        Text("Start", style = buttonTextStyle)
    }
}
