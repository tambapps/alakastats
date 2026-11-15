package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.check
import alakastats.composeapp.generated.resources.info
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.PokedexMove
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.SnackBar
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton
import com.tambapps.pokemon.alakastats.ui.composables.LazyVerticalGridWithScrollbar
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.SortChip
import com.tambapps.pokemon.alakastats.ui.composables.TypeBadge
import com.tambapps.pokemon.alakastats.ui.model.MoveTargetGroup
import com.tambapps.pokemon.alakastats.ui.model.PokedexPokemonSort
import com.tambapps.pokemon.alakastats.ui.model.PokedexPokemonSortCriterion
import com.tambapps.pokemon.alakastats.ui.model.label
import com.tambapps.pokemon.alakastats.ui.navigation.AppRoute
import com.tambapps.pokemon.alakastats.ui.navigation.LocalAppNavigator
import com.tambapps.pokemon.alakastats.ui.service.MoveTypeImage
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.theme.color
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

// stronger than the cards' of the lists: it's the screen's main card
private const val SUMMARY_TINT_LIGHT = 0.22f
private const val SUMMARY_TINT_DARK = 0.30f
private val SUMMARY_WIDTH = 400.dp
private val LEARNER_CARD_MIN_WIDTH = 300.dp

/**
 * The sheet of a move: its type, category, figures, target, classifications and description, then the Pokémon
 * learning it, which open their sheet. On desktop, the move is on the left of its Pokémon
 *
 * @param moveId the move's id in the Pokédex, e.g. trick-room
 */
@Composable
fun PokedexMoveScreen(moveId: String) {
    val viewModel = koinViewModel<PokedexMoveViewModel>()
    val navigator = LocalAppNavigator.current
    val snackBar = LocalSnackBar.current
    val isCompact = LocalIsCompact.current
    LaunchedEffect(moveId) { viewModel.load(moveId) }
    // a move can come from an URL, so it may not exist
    if (viewModel.isUnknown) {
        LaunchedEffect(moveId) {
            snackBar.show("Unknown move: $moveId", SnackBar.Severity.ERROR)
            navigator.pop()
        }
    }
    val gridState = rememberLazyGridState()
    // new filters or a new order start from the first Pokémon
    val shape = Triple(viewModel.sort, viewModel.stabOnly, viewModel.includeMegas)
    var shownShape by remember { mutableStateOf(shape) }
    LaunchedEffect(shape) {
        if (shape != shownShape) {
            shownShape = shape
            gridState.scrollToItem(0)
        }
    }
    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(if (isCompact) Modifier.safeContentPadding() else Modifier.padding(all = 16.dp))
    ) {
        Box(Modifier.fillMaxWidth()) {
            BackIconButton(navigator, Modifier.align(Alignment.CenterStart))
            Text(
                "Move",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Spacer(Modifier.height(12.dp))
        val move = viewModel.move
        when {
            viewModel.isLoading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            viewModel.loadingFailed -> CenteredText("The Pokédex couldn't be loaded")
            move == null -> Unit
            // one scroll for the move and its Pokémon, the screen being too narrow to put them side by side
            isCompact -> LearnersGrid(viewModel, gridState, isCompact = true, Modifier.padding(horizontal = 8.dp)) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        MoveSummary(move)
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
            else -> Row(
                Modifier.fillMaxSize().widthIn(max = 1600.dp).align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(Modifier.width(SUMMARY_WIDTH).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    MoveSummary(move)
                }
                LearnersGrid(viewModel, gridState, isCompact = false, Modifier.weight(1f))
            }
        }
    }
}

/**
 * The Pokémon learning the move: a title with their count, the filters and the sort, then their cards
 *
 * @param header items before the title (the move, on mobile)
 */
@Composable
private fun LearnersGrid(
    viewModel: PokedexMoveViewModel,
    state: LazyGridState,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
    header: LazyGridScope.() -> Unit = {},
) {
    PokedexPokemonGrid(
        viewModel.pokemons,
        showSpeed = viewModel.sort.criterion == PokedexPokemonSortCriterion.SPEED,
        state = state,
        isCompact = isCompact,
        emptyText = if (viewModel.learnerCount == 0) "No Pokémon learns this move" else "No Pokémon matches the filters",
        modifier = modifier,
        tag = { if (viewModel.isStab(it)) "STAB" else null },
    ) {
        header()
        item(span = { GridItemSpan(maxLineSpan) }) { LearnersControls(viewModel) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LearnersControls(viewModel: PokedexMoveViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Learned by ${viewModel.learnerCount} Pokémon",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            ToggleChip("STAB only", viewModel.stabOnly) { viewModel.stabOnly = it }
            ToggleChip("Include Megas", viewModel.includeMegas) { viewModel.includeMegas = it }
            val sort = viewModel.sort
            SortChip(
                criteria = PokedexPokemonSortCriterion.entries,
                criterion = sort.criterion,
                descending = sort.descending,
                isDefault = sort.isDefault,
                onSortChange = { criterion, descending -> viewModel.sort = PokedexPokemonSort(criterion, descending) },
                onReset = { viewModel.sort = PokedexPokemonSort() },
                resetLabel = "Reset to A → Z",
            )
        }
        if (viewModel.stabOnly || viewModel.includeMegas) {
            Text(
                "${viewModel.pokemons.size} shown",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}


/** The move's card: name, type and category, its figures as tiles, then its target, classifications and description */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoveSummary(move: PokedexMove) {
    val surface = MaterialTheme.colorScheme.surface
    val typeColor = move.type.color
    val tint = if (surface.luminance() < 0.5f) SUMMARY_TINT_DARK else SUMMARY_TINT_LIGHT
    MyCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = typeColor.copy(alpha = tint).compositeOver(surface)),
        border = BorderStroke(1.dp, typeColor.copy(alpha = 0.5f)),
        elevation = CardDefaults.elevatedCardElevation(0.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(move.name.en, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        TypeBadge(move.type)
                        DamageClassImage(move.damageClass, Modifier.size(width = 30.dp, height = 22.dp))
                        Text(move.damageClass.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
                MoveTypeImage(move.type, Modifier.size(48.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FigureTile("Power", move.basePower.takeIf { it > 0 }?.toString() ?: "—", Modifier.weight(1f))
                FigureTile("Accuracy", move.accuracy?.let { "$it%" } ?: "—", Modifier.weight(1f), caption = if (move.accuracy == null) "Never misses" else null)
                FigureTile("PP", move.pp.toString(), Modifier.weight(1f))
                FigureTile(
                    "Priority",
                    if (move.priority > 0) "+${move.priority}" else move.priority.toString(),
                    Modifier.weight(1f),
                    highlighted = move.priority != 0,
                )
            }
            HorizontalDivider()
            InfoRow("Target", MoveTargetGroup.of(move)?.label ?: move.target)
            InfoRow("Contact", if (move.makesContact) "Makes contact" else "No contact")
            if (move.classifications.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InfoLabel("Classes")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        move.classifications.forEach { ClassificationTag(it.label) }
                    }
                }
            }
            HorizontalDivider()
            Text(move.desc ?: move.shortDesc ?: "", style = MaterialTheme.typography.bodyMedium)
            if (move.championsModified) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(Res.drawable.info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Changed in Pokémon Champions",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** A figure of the move, e.g. its power, big under its name */
@Composable
private fun FigureTile(label: String, value: String, modifier: Modifier = Modifier, caption: String? = null, highlighted: Boolean = false) {
    val valueColor = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1)
        if (caption != null) {
            Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        InfoLabel(label)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InfoLabel(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.width(80.dp),
    )
}

@Composable
private fun ClassificationTag(text: String) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

