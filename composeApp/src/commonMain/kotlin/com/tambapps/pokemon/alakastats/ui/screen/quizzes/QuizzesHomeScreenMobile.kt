package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton

@Composable
internal fun QuizzesHomeScreenMobile(viewModel: QuizzesViewModel) {
    val navigator = LocalNavigator.currentOrThrow
    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .safeContentPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Box(Modifier.fillMaxWidth()) {
                BackIconButton(navigator, Modifier.align(Alignment.CenterStart))
                Text(
                    "Quizzes",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(Modifier.height(16.dp))

            for (quiz in viewModel.quizzes) {
                QuizCard(quiz, Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
