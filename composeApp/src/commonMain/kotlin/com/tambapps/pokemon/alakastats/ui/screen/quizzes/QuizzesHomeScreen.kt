package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.QuizType
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.elevatedCardGradientColors
import com.tambapps.pokemon.alakastats.ui.navigation.AppRoute
import com.tambapps.pokemon.alakastats.ui.navigation.LocalAppNavigator
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun QuizzesHomeScreen() {
    val viewModel = koinViewModel<QuizzesViewModel>()
    val isCompact = LocalIsCompact.current

    if (isCompact) {
        QuizzesHomeScreenMobile(viewModel)
    } else {
        QuizzesHomeScreenDesktop(viewModel)
    }
}

@Composable
internal fun QuizCard(quiz: QuizType, modifier: Modifier = Modifier) {
    val navigator = LocalAppNavigator.current
    MyCard(
        modifier = modifier,
        onClick = {
            navigator.push(
                when (quiz) {
                    QuizType.NATURES -> AppRoute.NatureQuizSetup
                    QuizType.SPEED_STAT -> AppRoute.SpeedStatQuizSetup
                    QuizType.SPEED_INTERACTIONS -> AppRoute.SpeedInteractionsHomeSetup
                }
            )
        },
        gradientBackgroundColors = elevatedCardGradientColors
    ) {
        Text(
            quiz.displayedName,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
    }
}
