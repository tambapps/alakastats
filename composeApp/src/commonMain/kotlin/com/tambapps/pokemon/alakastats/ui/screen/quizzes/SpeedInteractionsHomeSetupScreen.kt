package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.ui.navigation.LocalAppNavigator
import com.tambapps.pokemon.alakastats.ui.screen.home.buttonTextStyle
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SpeedInteractionsHomeSetupScreen() {
    val viewModel = koinViewModel<SpeedInteractionsHomeSetupViewModel>()
    val isCompact = LocalIsCompact.current

    if (isCompact) {
        SpeedInteractionsHomeSetupScreenMobile(viewModel)
    } else {
        SpeedInteractionsHomeSetupScreenDesktop(viewModel)
    }
}

@Composable
internal fun StartSpeedInteractionsQuizButton(viewModel: SpeedInteractionsHomeSetupViewModel, modifier: Modifier = Modifier) {
    val navigator = LocalAppNavigator.current
    Button(
        onClick = { viewModel.startQuiz(navigator) },
        enabled = viewModel.format != Format.NONE,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
    ) {
        Text("Start", style = buttonTextStyle)
    }
}
