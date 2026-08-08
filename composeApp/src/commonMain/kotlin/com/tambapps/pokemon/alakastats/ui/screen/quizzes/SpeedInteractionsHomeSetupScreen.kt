package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.ui.screen.home.buttonTextStyle
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact

object SpeedInteractionsHomeSetupScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SpeedInteractionsHomeSetupViewModel>()
        val isCompact = LocalIsCompact.current

        if (isCompact) {
            SpeedInteractionsHomeSetupScreenMobile(viewModel)
        } else {
            SpeedInteractionsHomeSetupScreenDesktop(viewModel)
        }
    }
}

@Composable
internal fun StartSpeedInteractionsQuizButton(viewModel: SpeedInteractionsHomeSetupViewModel, modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.currentOrThrow
    Button(
        onClick = { viewModel.startQuiz(navigator) },
        enabled = viewModel.format != Format.NONE,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
    ) {
        Text("Start", style = buttonTextStyle.copy(color = LocalContentColor.current))
    }
}
