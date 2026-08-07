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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.Stat
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton
import com.tambapps.pokemon.alakastats.ui.composables.LOOSE_COLOR
import com.tambapps.pokemon.alakastats.ui.composables.WIN_COLOR
import com.tambapps.pokemon.alakastats.ui.composables.WheelPickerDialog
import com.tambapps.pokemon.alakastats.ui.screen.home.buttonTextStyle
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import org.koin.core.parameter.parametersOf

data class NatureQuizScreen(val quizNatures: List<Nature>, val direction: QuizDirection) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<NatureQuizViewModel> { parametersOf(quizNatures, direction) }
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
                    "Natures Quiz",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(Modifier.height(24.dp))

            val contentModifier = if (isCompact) Modifier.fillMaxWidth() else Modifier.widthIn(max = 480.dp)
            if (viewModel.isFinished) {
                QuizResultsContent(viewModel, navigator, contentModifier)
            } else {
                QuizQuestionContent(viewModel, contentModifier)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun QuizQuestionContent(viewModel: NatureQuizViewModel, modifier: Modifier) {
    val nature = viewModel.currentNature ?: return
    val outcome = viewModel.lastOutcome
    val answered = outcome != null

    // Fallback focus holder: once the guess field/stat pickers get disabled after
    // answering, nothing else claims focus, so Enter would otherwise stop doing anything.
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
        Spacer(Modifier.height(24.dp))

        when (viewModel.direction) {
            QuizDirection.NAME_TO_STATS -> {
                Text(
                    nature.displayName,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "What stat does this nature affect?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            QuizDirection.STATS_TO_NAME -> {
                NatureStatsCaption(
                    nature,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Which nature is this?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(32.dp))

        when (viewModel.direction) {
            QuizDirection.NAME_TO_STATS -> {
                if (LocalIsCompact.current) {
                    StatGuessButtons(viewModel, enabled = !answered)
                } else {
                    StatChipMatrix(viewModel, enabled = !answered)
                }
            }
            QuizDirection.STATS_TO_NAME -> {
                val focusRequester = remember { FocusRequester() }
                val keyboardController = LocalSoftwareKeyboardController.current
                LaunchedEffect(viewModel.currentIndex) {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
                GuessTextField(
                    value = viewModel.guess,
                    onValueChange = { viewModel.guess = it },
                    placeholder = "e.g. Timid",
                    enabled = !answered,
                    onSubmit = { viewModel.submit() },
                    focusRequester = focusRequester,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
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
            AnswerFeedback(nature, outcome, viewModel.direction)
            Spacer(Modifier.height(24.dp))
            Button(onClick = { viewModel.nextQuestion() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (viewModel.isLastQuestion) "See results" else "Continue", color = LocalContentColor.current)
            }
        }
    }
}

@Composable
private fun AnswerFeedback(nature: Nature, outcome: AnswerOutcome, direction: QuizDirection) {
    val (message, color) = when (outcome) {
        AnswerOutcome.CORRECT -> "Correct!" to WIN_COLOR
        AnswerOutcome.INCORRECT -> "Incorrect" to LOOSE_COLOR
        AnswerOutcome.PASSED -> "Passed" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        if (outcome != AnswerOutcome.CORRECT) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Correct answer: ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                when (direction) {
                    QuizDirection.NAME_TO_STATS -> NatureStatsCaption(nature)
                    QuizDirection.STATS_TO_NAME -> Text(
                        nature.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizResultsContent(viewModel: NatureQuizViewModel, navigator: Navigator, modifier: Modifier) {
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
                        Text(result.nature.displayName, style = MaterialTheme.typography.bodyLarge)
                        NatureStatsCaption(result.nature)
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

private val statPickerItems: List<Stat?> = listOf(
    Stat.ATTACK, Stat.DEFENSE, null, Stat.SPECIAL_ATTACK, Stat.SPECIAL_DEFENSE, Stat.SPEED
)
private val statPickerNoneIndex = statPickerItems.indexOf(null)

@Composable
private fun StatGuessButtons(viewModel: NatureQuizViewModel, enabled: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatPickerButton(
            label = "Boosted",
            labelColor = increasedStatColor,
            selectedStat = viewModel.selectedIncreasedStat,
            hasPicked = viewModel.hasPickedIncreasedStat,
            enabled = enabled,
            onPicked = { viewModel.pickIncreasedStat(it) },
            modifier = Modifier.weight(1f)
        )
        StatPickerButton(
            label = "Lowered",
            labelColor = decreasedStatColor,
            selectedStat = viewModel.selectedDecreasedStat,
            hasPicked = viewModel.hasPickedDecreasedStat,
            enabled = enabled,
            onPicked = { viewModel.pickDecreasedStat(it) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatPickerButton(
    label: String,
    labelColor: Color,
    selectedStat: Stat?,
    hasPicked: Boolean,
    enabled: Boolean,
    onPicked: (Stat?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    val valueLabel = if (hasPicked) (selectedStat?.shortLabel ?: "None") else "?"

    OutlinedButton(onClick = { showPicker = true }, enabled = enabled, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = labelColor)
            Text(valueLabel, style = buttonTextStyle)
        }
    }

    if (showPicker) {
        WheelPickerDialog(
            title = label,
            items = statPickerItems,
            initialIndex = statPickerNoneIndex,
            itemToText = { it?.shortLabel ?: "None" },
            onPicked = onPicked,
            onDismissRequest = { showPicker = false }
        )
    }
}

private val statChipOptions: List<Stat?> = statOrder + listOf(null)

@Composable
private fun StatChipMatrix(viewModel: NatureQuizViewModel, enabled: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        StatSegmentedRow(
            label = "Boosted",
            labelColor = increasedStatColor,
            selectedStat = viewModel.selectedIncreasedStat,
            enabled = enabled,
            onPicked = { viewModel.pickIncreasedStat(it) }
        )
        StatSegmentedRow(
            label = "Lowered",
            labelColor = decreasedStatColor,
            selectedStat = viewModel.selectedDecreasedStat,
            enabled = enabled,
            onPicked = { viewModel.pickDecreasedStat(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatSegmentedRow(
    label: String,
    labelColor: Color,
    selectedStat: Stat?,
    enabled: Boolean,
    onPicked: (Stat?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = labelColor)
        SingleChoiceSegmentedButtonRow {
            statChipOptions.forEachIndexed { index, stat ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = statChipOptions.size),
                    modifier = Modifier.width(300.dp),
                    onClick = { onPicked(stat) },
                    selected = selectedStat == stat,
                    enabled = enabled,
                    icon = {},
                    label = {
                        Text(stat?.abbreviation ?: "None", maxLines = 1, softWrap = false)
                    }
                )
            }
        }
    }
}

@Composable
private fun GuessTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    onSubmit: () -> Unit,
    focusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        label = { Text("Your guess") },
        placeholder = { Text(placeholder) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
    )
}
