package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.swap_horiz
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.cardGradientColors
import com.tambapps.pokemon.alakastats.ui.theme.teamlyticsTabPaddingBottom
import org.jetbrains.compose.resources.painterResource

private const val SCROLL_TO_TOP_DURATION_MILLIS = 300

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
    Column(
        modifier.fillMaxSize()
            .verticalScroll(scrollState)
            .padding(12.dp)
            .padding(bottom = teamlyticsTabPaddingBottom),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DesktopResultCard(viewModel)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            DamageCalcPokemonColumn(
                state = viewModel.attacker,
                side = DamageCalcSide.ATTACKER,
                team = team,
                opponent = viewModel.defender,
                modifier = Modifier.weight(1.15f),
            )
            // no type tint on the field, between the two pokemon
            MyCard(modifier = Modifier.weight(0.9f), gradientBackgroundColors = cardGradientColors) {
                DamageCalcFieldPanel(
                    field = viewModel.field,
                    onFieldChange = { viewModel.updateField(it) },
                    modifier = Modifier.padding(12.dp),
                )
            }
            DamageCalcPokemonColumn(
                state = viewModel.defender,
                side = DamageCalcSide.DEFENDER,
                team = team,
                opponent = viewModel.attacker,
                modifier = Modifier.weight(1.15f),
            )
        }
    }
}

/**
 * The result of the selected move (its description, copied), and the results of every move of both pokemon to select one
 */
@Composable
private fun DesktopResultCard(viewModel: DamageCalcViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                (result as? DamageCalcResult.Success)?.let { CopyCalcButton(it) }
                IconButton(onClick = { viewModel.swap() }) {
                    Icon(
                        painter = painterResource(Res.drawable.swap_horiz),
                        contentDescription = "Swap attacker and defender"
                    )
                }
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
            val moveResults = viewModel.moveResults
            DamageCalcSide.entries.forEach { side ->
                val pokemon = viewModel.pokemonState(side)
                Text(
                    "${pokemon.form.pretty}'s moves",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    moveResults[side].orEmpty().forEachIndexed { index, moveResult ->
                        MoveResultCard(
                            moveName = pokemon.moves.getOrNull(index).orEmpty(),
                            result = moveResult,
                            isSelected = viewModel.selectedMoveSide == side && viewModel.selectedMoveIndex == index,
                            onClick = { viewModel.selectMove(side, index) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoveResultCard(
    moveName: String,
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
        Column(Modifier.padding(8.dp)) {
            Text(
                moveName.ifBlank { "-" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            when (result) {
                is DamageCalcResult.Success -> {
                    Text(result.damagePercentText, style = MaterialTheme.typography.bodyMedium)
                    DamageRangeBar(
                        currentHpFraction = result.currentHpFraction,
                        damageFractionRange = result.damageFractionRange,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
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
