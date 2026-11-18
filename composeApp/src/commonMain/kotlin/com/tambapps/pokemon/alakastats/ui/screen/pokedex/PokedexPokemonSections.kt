package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.close
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.DamageClass
import com.tambapps.pokemon.alakastats.domain.model.PokedexAbility
import com.tambapps.pokemon.alakastats.domain.model.PokedexMove
import com.tambapps.pokemon.alakastats.domain.model.PokedexSpecies
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.searchKeyboardActions
import com.tambapps.pokemon.alakastats.ui.composables.searchKeyboardOptions
import com.tambapps.pokemon.alakastats.ui.composables.SortChip
import com.tambapps.pokemon.alakastats.ui.composables.TypeFilterChip
import com.tambapps.pokemon.alakastats.ui.model.PokedexMoveSort
import com.tambapps.pokemon.alakastats.ui.model.PokedexMoveSortCriterion
import com.tambapps.pokemon.alakastats.ui.model.label
import com.tambapps.pokemon.alakastats.ui.navigation.AppRoute
import com.tambapps.pokemon.alakastats.ui.navigation.LocalAppNavigator
import com.tambapps.pokemon.alakastats.ui.service.MoveTypeImage
import com.tambapps.pokemon.alakastats.ui.theme.color
import org.jetbrains.compose.resources.painterResource

// the columns of the moves' figures
private val POWER_WIDTH = 44.dp
private val ACCURACY_WIDTH = 48.dp
private val PP_WIDTH = 32.dp

/**
 * The abilities of a Pokémon, each with what it does. The hidden one isn't told apart: in Champions, any of them can
 * be picked
 */
@Composable
internal fun AbilitiesSection(species: PokedexSpecies, modifier: Modifier = Modifier) {
    MyCard(modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Abilities")
            val navigator = LocalAppNavigator.current
            species.allAbilities.forEach { ability ->
                AbilityRow(ability) { navigator.push(AppRoute.PokedexAbility(ability.id)) }
            }
        }
    }
}

/** An ability and what it does, opening its sheet */
@Composable
private fun AbilityRow(ability: PokedexAbility, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(ability.name.en, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            ability.shortDesc ?: ability.desc ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The moves a Pokémon learns: a search, filters by category and type and a sort, then a row for each move, opening
 * its sheet
 *
 * @param showPp whether the moves' PP are shown, there being room for them
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MovepoolSection(viewModel: PokedexViewModel, species: PokedexSpecies, showPp: Boolean, modifier: Modifier = Modifier) {
    val navigator = LocalAppNavigator.current
    MyCard(modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Moves")
                Spacer(Modifier.width(8.dp))
                Text(
                    if (viewModel.moves.size == species.moves.size) "${species.moves.size}" else "${viewModel.moves.size} of ${species.moves.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = viewModel.moveQuery,
                onValueChange = { viewModel.moveQuery = it },
                placeholder = { Text("Search a move...", maxLines = 1) },
                singleLine = true,
                keyboardOptions = searchKeyboardOptions,
                keyboardActions = searchKeyboardActions(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    if (viewModel.moveQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.moveQuery = "" }) {
                            Icon(painterResource(Res.drawable.close), contentDescription = "Clear the search")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = viewModel.moveCategory == null,
                    onClick = { viewModel.moveCategory = null },
                    label = { Text("All") },
                )
                DamageClass.entries.forEach { category ->
                    FilterChip(
                        selected = viewModel.moveCategory == category,
                        // a second click goes back to all of them
                        onClick = { viewModel.moveCategory = if (viewModel.moveCategory == category) null else category },
                        label = { Text(category.label) },
                        leadingIcon = { DamageClassImage(category, Modifier.size(18.dp)) },
                    )
                }
                TypeFilterChip(viewModel.moveType, onTypeChange = { viewModel.moveType = it }, types = viewModel.moveTypes)
                val sort = viewModel.moveSort
                SortChip(
                    criteria = PokedexMoveSortCriterion.entries,
                    criterion = sort.criterion,
                    descending = sort.descending,
                    isDefault = sort.isDefault,
                    onSortChange = { criterion, descending -> viewModel.moveSort = PokedexMoveSort(criterion, descending) },
                    onReset = { viewModel.moveSort = PokedexMoveSort() },
                    resetLabel = "Reset to A → Z",
                )
            }
            MoveHeaderRow(showPp)
            if (viewModel.moves.isEmpty()) {
                Text(
                    "No move matches the search",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
            // a plain column: the sheet scrolls as a whole, and a Pokémon learns about a hundred moves at most
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                viewModel.moves.forEach { move ->
                    MoveRow(move, showPp = showPp) {
                        navigator.push(AppRoute.PokedexMove(move.id))
                    }
                }
            }
        }
    }
}

@Composable
private fun MoveHeaderRow(showPp: Boolean) {
    Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        HeaderText("Move", Modifier.weight(1f), TextAlign.Start)
        HeaderText("Power", Modifier.width(POWER_WIDTH))
        HeaderText("Acc.", Modifier.width(ACCURACY_WIDTH))
        if (showPp) HeaderText("PP", Modifier.width(PP_WIDTH))
    }
}

@Composable
private fun HeaderText(text: String, modifier: Modifier, textAlign: TextAlign = TextAlign.Center) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign,
        modifier = modifier,
    )
}

/** A move on a tint of its type's color: its type, name, priority and category, then its power, accuracy and PP */
@Composable
private fun MoveRow(move: PokedexMove, showPp: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(move.type.color.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoveTypeImage(move.type, Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                move.name.en,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (move.priority != 0) {
                Spacer(Modifier.width(6.dp))
                SmallTag(if (move.priority > 0) "+${move.priority}" else move.priority.toString(), MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.width(6.dp))
        DamageClassImage(move.damageClass, Modifier.size(width = 26.dp, height = 18.dp))
        Figure(move.basePower.takeIf { it > 0 }?.toString() ?: "—", POWER_WIDTH)
        // a move that can't miss
        Figure(move.accuracy?.let { "$it%" } ?: "—", ACCURACY_WIDTH)
        if (showPp) Figure(move.pp.toString(), PP_WIDTH)
    }
}

@Composable
private fun Figure(text: String, width: Dp) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier.width(width),
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun SmallTag(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}
