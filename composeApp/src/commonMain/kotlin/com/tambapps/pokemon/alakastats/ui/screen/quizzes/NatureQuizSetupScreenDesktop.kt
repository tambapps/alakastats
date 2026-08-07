package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
internal fun NatureQuizSetupScreenDesktop(viewModel: NatureQuizSetupViewModel) {
    val navigator = LocalNavigator.currentOrThrow
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(all = 16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth()) {
            BackIconButton(navigator, Modifier.align(Alignment.CenterStart))
            Text(
                "Natures",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Spacer(Modifier.height(16.dp))

        IgnoreNaturesButton(viewModel)
        Spacer(Modifier.height(16.dp))

        NatureGrid(viewModel)
        Spacer(Modifier.height(16.dp))

        StartQuizButton(viewModel)
    }
}
