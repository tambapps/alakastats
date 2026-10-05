package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.ui.service.MoveTypeImage
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.cardGradientColors
import com.tambapps.pokemon.alakastats.ui.theme.teamlyticsTabPaddingBottom

private const val SCROLL_TO_TOP_DURATION_MILLIS = 300
private val PADDING = 12.dp
private const val USELESS_MOVE_ALPHA = 0.5f
private const val MOVES_PER_ROW = 2
private val MOVE_HALVES_SPACING = 40.dp

/**
 * The damage calc on wide screens: the results of every move of both pokemon on top, then the attacker, the field and
 * the defender side by side
 */
@Composable
internal fun DamageCalcDesktop(
    viewModel: DamageCalcViewModel,
    modifier: Modifier = Modifier,
    team: Teamlytics? = null,
) {
    val scrollState = rememberScrollState()
    viewModel.scrollToTopSignal.Listen {
        if (scrollState.value != 0) {
            scrollState.animateScrollTo(0, tween(SCROLL_TO_TOP_DURATION_MILLIS))
        }
    }
    // the bottom of the result card in the scrolled column, to show the compact result once it's scrolled away
    val paddingPx = with(LocalDensity.current) { PADDING.roundToPx() }
    var resultCardHeight by remember { mutableIntStateOf(0) }
    val isResultCardScrolledAway by remember {
        derivedStateOf { resultCardHeight > 0 && scrollState.value > paddingPx + resultCardHeight }
    }
    Box(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(scrollState)
                .padding(PADDING)
                .padding(bottom = teamlyticsTabPaddingBottom),
            verticalArrangement = Arrangement.spacedBy(PADDING)
        ) {
            DesktopResultCard(viewModel, Modifier.onSizeChanged { resultCardHeight = it.height })
            DesktopEditors(viewModel, team)
        }
        // pinned while editing what's under the result card, to see the result change
        AnimatedVisibility(
            visible = isResultCardScrolledAway,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            DesktopCompactResult(viewModel)
        }
    }
}

@Composable
private fun DesktopEditors(viewModel: DamageCalcViewModel, team: Teamlytics?) {
    Row(horizontalArrangement = Arrangement.spacedBy(PADDING), verticalAlignment = Alignment.Top) {
        DamageCalcPokemonColumn(
            state = viewModel.attacker,
            side = DamageCalcSide.ATTACKER,
            team = team,
            opponent = viewModel.defender,
            field = viewModel.field,
            modifier = Modifier.weight(1.15f),
        )
        // no type tint on the field, between the two pokemon
        MyCard(modifier = Modifier.weight(0.9f), gradientBackgroundColors = cardGradientColors) {
            DamageCalcFieldPanel(
                field = viewModel.field,
                onFieldChange = { viewModel.updateField(it) },
                modifier = Modifier.padding(12.dp),
                sideName = { it.desktopName },
            )
        }
        DamageCalcPokemonColumn(
            state = viewModel.defender,
            side = DamageCalcSide.DEFENDER,
            team = team,
            opponent = viewModel.attacker,
            field = viewModel.field,
            modifier = Modifier.weight(1.15f),
        )
    }
}

/**
 * The selected move's result in one line, with its damage bar, pinned on top once the result card is scrolled away
 */
@Composable
private fun DesktopCompactResult(viewModel: DamageCalcViewModel) {
    val result = viewModel.result
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = PADDING, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // the column headers showing the pokemon are scrolled away too
            CalcPokemonSprites(viewModel)
            viewModel.moveTypeOf(viewModel.selectedMoveSide, viewModel.selectedMoveIndex)?.let {
                MoveTypeImage(it, Modifier.size(MOVE_TYPE_ICON_SIZE))
            }
            val moveName = viewModel.pokemonState(viewModel.selectedMoveSide).moves
                .getOrNull(viewModel.selectedMoveIndex).orEmpty()
            Text(
                moveName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            when (result) {
                is DamageCalcResult.Success -> {
                    Text(result.shownDamageText, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    // no bar when there's no damage to show
                    if (result.dealsNoDamage) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        DamageRangeBar(
                            currentHpFraction = result.currentHpFraction,
                            damageFractionRange = result.damageFractionRange,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        result.koChanceText,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    CopyCalcButton(result)
                }
                is DamageCalcResult.Error -> Text(
                    result.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * The result of the selected move (its description, copied), and the results of every move of both pokemon to select one
 */
@Composable
private fun DesktopResultCard(viewModel: DamageCalcViewModel, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val result = viewModel.result
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    calcTitle(viewModel, viewModel.selectedMoveSide),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                // no swap button: the moves of both pokemon are already calculated
                (result as? DamageCalcResult.Success)?.let { CopyCalcButton(it) }
            }
            when (result) {
                // the text copied, selectable to copy only a part of it
                is DamageCalcResult.Success -> SelectionContainer {
                    Text(result.description, style = MaterialTheme.typography.bodyLarge)
                }
                is DamageCalcResult.Error -> Text(
                    result.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
            // each pokemon's moves above its column: pokemon 1's on the left, pokemon 2's on the right
            Row(Modifier.height(IntrinsicSize.Min)) {
                SideMoveResults(viewModel, DamageCalcSide.ATTACKER, Modifier.weight(1f))
                // separating the two pokemon, like the field's sides
                VerticalDivider(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.padding(horizontal = MOVE_HALVES_SPACING / 2),
                )
                SideMoveResults(viewModel, DamageCalcSide.DEFENDER, Modifier.weight(1f))
            }
        }
    }
}

/**
 * The results of the moves of the [side] pokemon, 2 per row in the order of its moves, to select one
 */
@Composable
private fun SideMoveResults(viewModel: DamageCalcViewModel, side: DamageCalcSide, modifier: Modifier = Modifier) {
    val pokemon = viewModel.pokemonState(side)
    val moveResults = viewModel.moveResults[side].orEmpty()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "${pokemon.form.pretty}'s moves",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        moveResults.indices.chunked(MOVES_PER_ROW).forEach { indexes ->
            // cards of equal height, even for a move without result (shorter)
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                indexes.forEach { index ->
                    MoveResultCard(
                        moveName = pokemon.moves.getOrNull(index).orEmpty(),
                        moveType = viewModel.moveTypeOf(side, index),
                        result = moveResults[index],
                        isSelected = viewModel.selectedMoveSide == side && viewModel.selectedMoveIndex == index,
                        onClick = { viewModel.selectMove(side, index) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                // a last row with a single move keeps the cards' width
                repeat(MOVES_PER_ROW - indexes.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MoveResultCard(
    moveName: String,
    moveType: PokeType?,
    result: DamageCalcResult,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else CardDefaults.outlinedCardBorder(),
    ) {
        // dimmed when the move deals no damage (an immunity, a status move...), so that the moves that do stand out
        val isUseless = result is DamageCalcResult.Error || (result as? DamageCalcResult.Success)?.dealsNoDamage == true
        Column(Modifier.padding(8.dp).alpha(if (isUseless) USELESS_MOVE_ALPHA else 1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (moveType != null) {
                    MoveTypeImage(moveType, Modifier.size(MOVE_TYPE_ICON_SIZE))
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    moveName.ifBlank { "-" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            when (result) {
                is DamageCalcResult.Success -> {
                    Text(result.shownDamageText, style = MaterialTheme.typography.bodyMedium)
                    if (!result.dealsNoDamage) {
                        DamageRangeBar(
                            currentHpFraction = result.currentHpFraction,
                            damageFractionRange = result.damageFractionRange,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                    }
                    Text(result.koChanceText, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
                is DamageCalcResult.Error -> Text(
                    result.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}
