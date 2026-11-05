package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.ui.navigation.LocalAppNavigator
import com.tambapps.pokemon.alakastats.ui.screen.home.buttonTextStyle
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SpeedStatQuizSetupScreen() {
    val viewModel = koinViewModel<SpeedStatQuizSetupViewModel>()
    val isCompact = LocalIsCompact.current

    if (isCompact) {
        SpeedStatQuizSetupScreenMobile(viewModel)
    } else {
        SpeedStatQuizSetupScreenDesktop(viewModel)
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
