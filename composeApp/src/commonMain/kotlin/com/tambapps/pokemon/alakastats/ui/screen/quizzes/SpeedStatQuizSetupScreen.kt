package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.ui.screen.home.buttonTextStyle
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact

object SpeedStatQuizSetupScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SpeedStatQuizSetupViewModel>()
        val isCompact = LocalIsCompact.current

        if (isCompact) {
            SpeedStatQuizSetupScreenMobile(viewModel)
        } else {
            SpeedStatQuizSetupScreenDesktop(viewModel)
        }
    }
}

@Composable
internal fun QuizOptionCheckbox(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun StartSpeedStatQuizButton(viewModel: SpeedStatQuizSetupViewModel, modifier: Modifier = Modifier) {
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
