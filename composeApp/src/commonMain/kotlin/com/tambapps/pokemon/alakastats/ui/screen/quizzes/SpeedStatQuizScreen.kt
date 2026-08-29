package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton
import com.tambapps.pokemon.alakastats.ui.composables.LOOSE_COLOR
import com.tambapps.pokemon.alakastats.ui.composables.WIN_COLOR
import com.tambapps.pokemon.alakastats.ui.service.PokemonArtwork
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import org.koin.core.parameter.parametersOf

data class SpeedStatQuizScreen(
    val format: Format,
    val allowChoiceScarf: Boolean,
    val useMaxStatPoints: Boolean,
    val useSpeedBoostingNature: Boolean
) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SpeedStatQuizViewModel> {
            parametersOf(format, allowChoiceScarf, useMaxStatPoints, useSpeedBoostingNature)
        }
        val navigator = LocalNavigator.currentOrThrow
        val isCompact = LocalIsCompact.current

        Column(
            modifier = Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .padding(horizontal = if (isCompact) 16.dp else 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.fillMaxWidth()) {
                BackIconButton(navigator, Modifier.align(Alignment.CenterStart))
                Text(
                    "Speed Stat Quiz",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(Modifier.height(24.dp))

            val contentModifier = if (isCompact) Modifier.fillMaxWidth() else Modifier.widthIn(max = 480.dp)
            when {
                viewModel.isLoading -> LoadingContent(contentModifier)
                viewModel.loadFailed -> LoadFailedContent(navigator, contentModifier)
                viewModel.isFinished -> SpeedResultsContent(viewModel, navigator, contentModifier)
                else -> SpeedQuestionContent(viewModel, contentModifier)
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(48.dp))
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            "Loading Pokemon...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoadFailedContent(navigator: Navigator, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(48.dp))
        Text(
            "Couldn't load Pokemon for this format. Please try again.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { navigator.pop() }) {
            Text("Back", color = LocalContentColor.current)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SpeedQuestionContent(viewModel: SpeedStatQuizViewModel, modifier: Modifier) {
    val question = viewModel.currentQuestion ?: return
    val outcome = viewModel.lastOutcome
    val answered = outcome != null

    val rootFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        rootFocusRequester.requestFocus()
    }
    LaunchedEffect(answered) {
        if (answered) {
            rootFocusRequester.requestFocus()
        }
    }

    Column(
        modifier = modifier
            .focusRequester(rootFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                    when {
                        answered -> {
                            viewModel.nextQuestion()
                            true
                        }
                        viewModel.canSubmit -> {
                            viewModel.submit()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Question ${viewModel.currentIndex + 1} of ${viewModel.questions.size}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            assumptionsCaption(viewModel.useMaxStatPoints, viewModel.useSpeedBoostingNature),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        PokemonArtwork(
            question.pokemonName,
            modifier = Modifier.size(if (LocalIsCompact.current) 160.dp else 220.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(question.pokemonName.pretty, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        if (question.holdsScarf) {
            Spacer(Modifier.height(8.dp))
            ScarfBadge()
        }
        Spacer(Modifier.height(24.dp))

        Text(
            "What's its Speed stat?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(viewModel.currentIndex) {
            focusRequester.requestFocus()
        }
        SpeedGuessTextField(
            value = viewModel.guess,
            onValueChange = { viewModel.guess = it.filter(Char::isDigit) },
            enabled = !answered,
            onSubmit = { viewModel.submit() },
            focusRequester = focusRequester,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(28.dp))

        if (!answered) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { viewModel.pass() }, modifier = Modifier.weight(1f)) {
                    Text("Pass")
                }
                Button(
                    onClick = { viewModel.submit() },
                    enabled = viewModel.canSubmit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Submit", color = LocalContentColor.current)
                }
            }
        } else {
            SpeedAnswerFeedback(question, outcome, viewModel.results.lastOrNull()?.guess)
            Spacer(Modifier.height(24.dp))
            Button(onClick = { viewModel.nextQuestion() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (viewModel.isLastQuestion) "See results" else "Continue", color = LocalContentColor.current)
            }
        }
    }
}

@Composable
private fun ScarfBadge() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Holding Choice Scarf (×1.5 Speed)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun SpeedAnswerFeedback(question: SpeedQuestion, outcome: AnswerOutcome, guessedValue: Int?) {
    val (message, color) = when (outcome) {
        AnswerOutcome.CORRECT -> "Correct!" to WIN_COLOR
        AnswerOutcome.INCORRECT -> "Incorrect" to LOOSE_COLOR
        AnswerOutcome.PASSED -> "Passed" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        if (outcome != AnswerOutcome.CORRECT) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Correct Speed: ${question.correctSpeed}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (outcome == AnswerOutcome.INCORRECT && guessedValue != null) {
                val diff = guessedValue - question.correctSpeed
                Text(
                    if (diff > 0) "Your guess was $diff too high" else "Your guess was ${-diff} too low",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SpeedResultsContent(viewModel: SpeedStatQuizViewModel, navigator: Navigator, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Quiz complete!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "${viewModel.correctCount} / ${viewModel.questions.size} correct",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        val toReview = viewModel.results.filter { it.outcome != AnswerOutcome.CORRECT }
        if (toReview.isNotEmpty()) {
            Text(
                "To review",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                toReview.forEach { result ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(result.question.pokemonName.pretty, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${result.guess ?: "—"} → ${result.question.correctSpeed}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { viewModel.restart() }, modifier = Modifier.weight(1f)) {
                Text("Retry")
            }
            Button(onClick = { navigator.pop() }, modifier = Modifier.weight(1f)) {
                Text("Done", color = LocalContentColor.current)
            }
        }
    }
}

@Composable
private fun SpeedGuessTextField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    onSubmit: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        label = { Text("Your guess") },
        placeholder = { Text("e.g. 187") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        modifier = modifier.focusRequester(focusRequester)
    )
}

private fun assumptionsCaption(useMaxStatPoints: Boolean, useSpeedBoostingNature: Boolean): String {
    val statPoints = if (useMaxStatPoints) "32 stat points" else "0 stat points"
    val nature = if (useSpeedBoostingNature) "Speed-boosting nature" else "Neutral nature"
    return "Assuming $statPoints · $nature"
}
