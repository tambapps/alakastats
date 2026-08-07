package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.tambapps.pokemon.alakastats.domain.model.QuizType
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.elevatedCardGradientColors
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact

object QuizzesHomeScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<QuizzesViewModel>()
        val isCompact = LocalIsCompact.current

        if (isCompact) {
            QuizzesHomeScreenMobile(viewModel)
        } else {
            QuizzesHomeScreenDesktop(viewModel)
        }
    }
}

@Composable
internal fun QuizCard(quiz: QuizType, modifier: Modifier = Modifier) {
    MyCard(
        modifier = modifier,
        enabled = true,
        gradientBackgroundColors = elevatedCardGradientColors
    ) {
        Text(
            quiz.displayedName,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
    }
}
