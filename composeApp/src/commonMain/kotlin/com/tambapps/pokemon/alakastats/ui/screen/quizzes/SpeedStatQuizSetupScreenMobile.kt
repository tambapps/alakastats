package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import com.tambapps.pokemon.alakastats.ui.composables.FormatSelector

@Composable
internal fun SpeedStatQuizSetupScreenMobile(viewModel: SpeedStatQuizSetupViewModel) {
    val navigator = LocalNavigator.currentOrThrow
    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .safeContentPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.fillMaxWidth()) {
                BackIconButton(navigator, Modifier.align(Alignment.CenterStart))
                Text(
                    "Speed Stat",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FormatSelector(
                    format = viewModel.format,
                    onFormatChange = viewModel::selectFormat,
                    modifier = Modifier.fillMaxWidth()
                )

                QuizOptionCheckbox(
                    label = "Choice Scarf",
                    description = "Some quizzed Pokemon may hold a Choice Scarf (×1.5 Speed)",
                    checked = viewModel.allowChoiceScarf,
                    onCheckedChange = { viewModel.toggleChoiceScarf() }
                )

                QuizOptionCheckbox(
                    label = "Max stat points",
                    description = "Assume 32 stat points invested in Speed",
                    checked = viewModel.useMaxStatPoints,
                    onCheckedChange = { viewModel.toggleMaxStatPoints() }
                )

                QuizOptionCheckbox(
                    label = "Speed-boosting nature",
                    description = "Assume a Speed-boosting nature (+10% Speed)",
                    checked = viewModel.useSpeedBoostingNature,
                    onCheckedChange = { viewModel.toggleSpeedBoostingNature() }
                )
            }
            Spacer(Modifier.height(24.dp))

            StartSpeedStatQuizButton(viewModel)
            Spacer(Modifier.height(16.dp))
        }
    }
}
