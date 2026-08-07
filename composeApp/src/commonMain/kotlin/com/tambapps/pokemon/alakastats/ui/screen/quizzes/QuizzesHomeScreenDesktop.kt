package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
internal fun QuizzesHomeScreenDesktop(viewModel: QuizzesViewModel) {
    val navigator = LocalNavigator.currentOrThrow
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(all = 16.dp)
            .fillMaxSize(),
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
        QuizCardGrid(viewModel, 3)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun QuizCardGrid(
    viewModel: QuizzesViewModel,
    columns: Int,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            viewModel.quizzes.forEach { quiz ->
                item(key = quiz.name) {
                    QuizCard(quiz = quiz)
                }
            }
        }
    }
}
