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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.domain.model.OpponentSpeedRange
import com.tambapps.pokemon.alakastats.domain.model.RevealedNature
import com.tambapps.pokemon.alakastats.domain.model.SpeedComparisonResult
import com.tambapps.pokemon.alakastats.domain.model.SpeedModifier
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton
import com.tambapps.pokemon.alakastats.ui.composables.LOOSE_COLOR
import com.tambapps.pokemon.alakastats.ui.composables.WIN_COLOR
import com.tambapps.pokemon.alakastats.ui.service.FacingDirection
import com.tambapps.pokemon.alakastats.ui.service.LocalPokemonImageService
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.theme.isDarkThemeEnabled
import org.koin.core.parameter.parametersOf

data class SpeedInteractionsQuizScreen(val mode: SpeedInteractionsMode) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SpeedInteractionsViewModel> { parametersOf(mode) }
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
                    "Speed Interactions",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(Modifier.height(24.dp))

            val contentModifier = if (isCompact) Modifier.fillMaxWidth() else Modifier.widthIn(max = 480.dp)
            when {
                viewModel.isLoading -> LoadingContent(contentModifier)
                viewModel.noFormatSet -> MessageContent(
                    "This team doesn't have a format set, so a meta pool of opponents can't be loaded.",
                    navigator,
                    contentModifier
                )
                viewModel.loadFailed -> MessageContent(
                    "Couldn't load Pokemon for this quiz. Please try again.",
                    navigator,
                    contentModifier
                )
                viewModel.isFinished -> SpeedInteractionsResultsContent(viewModel, navigator, contentModifier)
                else -> SpeedInteractionsQuestionContent(viewModel, contentModifier)
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
private fun MessageContent(message: String, navigator: Navigator, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(48.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { navigator.pop() }) {
            Text("Back", color = LocalContentColor.current)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SpeedInteractionsQuestionContent(viewModel: SpeedInteractionsViewModel, modifier: Modifier) {
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
                if (answered && event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                    viewModel.nextQuestion()
                    true
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
            assumptionsCaption(viewModel),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))

        MatchupHeader(viewModel, question)

        if (question.modifier != SpeedModifier.NONE) {
            Spacer(Modifier.height(12.dp))
            Text(
                question.modifier.caption(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(28.dp))

        Text(
            if (viewModel.isTeamMode) "Does your Pokemon outspeed the opponent's?" else "Which Pokemon is faster?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))

        if (viewModel.isTeamMode) {
            TeamAnswerButtons(viewModel, enabled = !answered)
        } else {
            HomeAnswerButtons(viewModel, question, enabled = !answered)
        }
        Spacer(Modifier.height(16.dp))

        if (!answered) {
            OutlinedButton(onClick = { viewModel.pass() }, modifier = Modifier.fillMaxWidth()) {
                Text("Pass")
            }
        } else {
            Spacer(Modifier.height(12.dp))
            SpeedInteractionsAnswerFeedback(question, outcome, viewModel.isTeamMode)
            Spacer(Modifier.height(24.dp))
            Button(onClick = { viewModel.nextQuestion() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (viewModel.isLastQuestion) "See results" else "Continue", color = LocalContentColor.current)
            }
        }
    }
}

@Composable
private fun MatchupHeader(viewModel: SpeedInteractionsViewModel, question: SpeedInteractionQuestion) {
    val pokemonImageService = LocalPokemonImageService.current
    val artworkSize = if (LocalIsCompact.current) 120.dp else 160.dp
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                pokemonImageService.PokemonArtwork(
                    question.pokemonA,
                    modifier = Modifier.size(artworkSize),
                    facingDirection = FacingDirection.RIGHT
                )
                if (question.itemA != null) {
                    pokemonImageService.ItemImage(
                        question.itemA,
                        modifier = Modifier.size(28.dp).align(Alignment.BottomEnd)
                    )
                }
            }
            Text(question.pokemonA.pretty, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (viewModel.isTeamMode) {
                Text("Yours", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("VS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            pokemonImageService.PokemonArtwork(question.pokemonB, modifier = Modifier.size(artworkSize))
            Text(question.pokemonB.pretty, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (viewModel.isTeamMode) {
                Text("Opponent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            question.opponentNature?.let { nature ->
                Text(
                    nature.label(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = nature.color()
                )
            }
        }
    }
}

@Composable
private fun HomeAnswerButtons(viewModel: SpeedInteractionsViewModel, question: SpeedInteractionQuestion, enabled: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AnswerButton("${question.pokemonA.pretty} faster", SpeedComparisonResult.FASTER, viewModel, enabled, Modifier.weight(1f))
        AnswerButton("Speed tie", SpeedComparisonResult.SPEED_TIE, viewModel, enabled, Modifier.weight(1f))
        AnswerButton("${question.pokemonB.pretty} faster", SpeedComparisonResult.SLOWER, viewModel, enabled, Modifier.weight(1f))
    }
}

@Composable
private fun TeamAnswerButtons(viewModel: SpeedInteractionsViewModel, enabled: Boolean) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnswerButton("Guaranteed faster", SpeedComparisonResult.GUARANTEED_FASTER, viewModel, enabled, Modifier.weight(1f))
            AnswerButton("Guaranteed slower", SpeedComparisonResult.GUARANTEED_SLOWER, viewModel, enabled, Modifier.weight(1f))
        }
        AnswerButton("Depends on investment", SpeedComparisonResult.DEPENDS_ON_INVESTMENT, viewModel, enabled, Modifier.fillMaxWidth())
    }
}

@Composable
private fun AnswerButton(
    label: String,
    result: SpeedComparisonResult,
    viewModel: SpeedInteractionsViewModel,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val isSelected = viewModel.selectedAnswer == result
    if (isSelected) {
        Button(onClick = {}, enabled = false, modifier = modifier) {
            Text(label, textAlign = TextAlign.Center)
        }
    } else {
        OutlinedButton(onClick = { viewModel.submit(result) }, enabled = enabled, modifier = modifier) {
            Text(label, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SpeedInteractionsAnswerFeedback(question: SpeedInteractionQuestion, outcome: AnswerOutcome, isTeamMode: Boolean) {
    val (message, color) = when (outcome) {
        AnswerOutcome.CORRECT -> "Correct!" to WIN_COLOR
        AnswerOutcome.INCORRECT -> "Incorrect" to LOOSE_COLOR
        AnswerOutcome.PASSED -> "Passed" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.height(4.dp))
        if (isTeamMode) {
            val range = question.opponentRange
            val nature = question.opponentNature
            Text(
                "Your ${question.pokemonA.pretty}'s Speed: ${question.speedA}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (range != null && nature != null) {
                Text(
                    "${question.pokemonB.pretty} (${nature.label()}) — possible Speed: ${range.min} (0 inv.) to ${range.max} (max inv.)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    teamModeExplanation(question, range),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Text(
                "${question.pokemonA.pretty}: ${question.speedA} · ${question.pokemonB.pretty}: ${question.speedB}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SpeedInteractionsResultsContent(viewModel: SpeedInteractionsViewModel, navigator: Navigator, modifier: Modifier) {
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
                        Text(
                            "${result.question.pokemonA.pretty} vs ${result.question.pokemonB.pretty}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            result.question.correctResult.label(),
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

private fun SpeedComparisonResult.label(): String = when (this) {
    SpeedComparisonResult.FASTER -> "Faster"
    SpeedComparisonResult.SLOWER -> "Slower"
    SpeedComparisonResult.SPEED_TIE -> "Speed tie"
    SpeedComparisonResult.GUARANTEED_FASTER -> "Guaranteed faster"
    SpeedComparisonResult.GUARANTEED_SLOWER -> "Guaranteed slower"
    SpeedComparisonResult.DEPENDS_ON_INVESTMENT -> "Depends on investment"
}

private fun RevealedNature.label(): String = when (this) {
    RevealedNature.BOOSTING -> "+Spe nature"
    RevealedNature.NEUTRAL -> "Neutral nature"
    RevealedNature.REDUCING -> "−Spe nature"
}

@Composable
private fun RevealedNature.color(): Color = when (this) {
    RevealedNature.BOOSTING -> if (isDarkThemeEnabled()) Color(0xFFE57373) else Color.Red
    RevealedNature.NEUTRAL -> Color.Unspecified
    RevealedNature.REDUCING -> if (isDarkThemeEnabled()) Color.Cyan else Color.Blue
}

private fun teamModeExplanation(question: SpeedInteractionQuestion, range: OpponentSpeedRange): String {
    val pokemonB = question.pokemonB.pretty
    val mine = question.speedA
    val base = when (question.correctResult) {
        SpeedComparisonResult.GUARANTEED_FASTER -> "Your $mine beats $pokemonB's max (${range.max}) — you always outspeed."
        SpeedComparisonResult.GUARANTEED_SLOWER -> "Your $mine is below $pokemonB's min (${range.min}) — they always outspeed you."
        SpeedComparisonResult.DEPENDS_ON_INVESTMENT ->
            "Your $mine is inside $pokemonB's range (${range.min}–${range.max}) — it depends on how much Speed they invested."
        else -> ""
    }
    val thresholdNote = question.investmentThreshold?.let { " $pokemonB outspeeds you from $it stat points invested." } ?: ""
    val boundaryNote = when (mine) {
        range.max -> " (you tie a fully-invested $pokemonB; anything less, you outspeed)"
        range.min -> " (you tie an uninvested $pokemonB; any investment outspeeds you)"
        else -> ""
    }
    return base + thresholdNote + boundaryNote
}

private fun SpeedModifier.caption(): String = when (this) {
    SpeedModifier.B_CHOICE_SCARF -> "Opponent could be holding a Choice Scarf"
    SpeedModifier.B_PARALYSIS -> "Opponent is paralyzed"
    SpeedModifier.A_PARALYSIS -> "You are paralyzed"
    SpeedModifier.A_TAILWIND -> "Under your Tailwind"
    SpeedModifier.NONE -> ""
}

private fun assumptionsCaption(viewModel: SpeedInteractionsViewModel): String {
    val formatName = viewModel.resolvedFormat?.displayedName ?: "..."
    return if (viewModel.isTeamMode) {
        formatName
    } else {
        "Level 50 · $formatName · Both uninvested (0 EV, neutral nature)"
    }
}
