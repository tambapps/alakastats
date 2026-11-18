package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.close
import alakastats.composeapp.generated.resources.move_physical
import alakastats.composeapp.generated.resources.move_special
import alakastats.composeapp.generated.resources.move_status
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.alakastats.domain.model.DamageClass
import com.tambapps.pokemon.alakastats.domain.model.MoveClassification
import com.tambapps.pokemon.alakastats.domain.model.PokedexMove
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton
import com.tambapps.pokemon.alakastats.ui.composables.searchKeyboardActions
import com.tambapps.pokemon.alakastats.ui.composables.searchKeyboardOptions
import com.tambapps.pokemon.alakastats.ui.composables.LazyVerticalGridWithScrollbar
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.SortChip
import com.tambapps.pokemon.alakastats.ui.composables.displayName
import com.tambapps.pokemon.alakastats.ui.model.MoveAccuracyRange
import com.tambapps.pokemon.alakastats.ui.model.MovePowerRange
import com.tambapps.pokemon.alakastats.ui.model.MoveTargetGroup
import com.tambapps.pokemon.alakastats.ui.model.PokedexMoveFilters
import com.tambapps.pokemon.alakastats.ui.model.PokedexMoveSort
import com.tambapps.pokemon.alakastats.ui.model.PokedexMoveSortCriterion
import com.tambapps.pokemon.alakastats.ui.model.label
import com.tambapps.pokemon.alakastats.ui.navigation.AppRoute
import com.tambapps.pokemon.alakastats.ui.navigation.LocalAppNavigator
import com.tambapps.pokemon.alakastats.ui.service.MoveTypeImage
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.theme.color
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

// the share of the type's color in a card's background, and in its border, like the Pokémon's cards
private const val CARD_TINT_LIGHT = 0.16f
private const val CARD_TINT_DARK = 0.24f
private const val CARD_BORDER_ALPHA = 0.45f
private val CARD_MIN_WIDTH = 360.dp
private val FILTERS_PANEL_WIDTH = 320.dp

/** The filters, in the order of the panel */
private enum class MoveFilterSection(val title: String) {
    CATEGORY("Category"), TYPE("Type"), CLASSIFICATION("Classification"), TARGET("Target"), PRIORITY("Priority"),
    PP("PP"), POWER("Power"), ACCURACY("Accuracy"),
}

/**
 * The moves of the Pokédex as cards in the color of their type, searched by name or description, filtered and sorted.
 * The filters are in a panel on the right on desktop, in a bottom sheet on mobile
 */
@Composable
fun PokedexMovesScreen() {
    val viewModel = koinViewModel<PokedexMovesViewModel>()
    val navigator = LocalAppNavigator.current
    val isCompact = LocalIsCompact.current
    LaunchedEffect(Unit) { viewModel.load() }
    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(if (isCompact) Modifier.safeContentPadding() else Modifier.padding(all = 16.dp))
    ) {
        Box(Modifier.fillMaxWidth()) {
            BackIconButton(navigator, Modifier.align(Alignment.CenterStart))
            Text(
                "Moves",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Spacer(Modifier.height(12.dp))
        if (isCompact) {
            Column(Modifier.padding(horizontal = 8.dp)) {
                MovesContent(viewModel, isCompact = true)
            }
        } else {
            Row(
                Modifier.fillMaxSize().widthIn(max = 1600.dp).align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    MovesContent(viewModel, isCompact = false)
                }
                MyCard(
                    modifier = Modifier.width(FILTERS_PANEL_WIDTH).fillMaxHeight(),
                    elevation = CardDefaults.elevatedCardElevation(2.dp),
                ) {
                    FiltersPanel(viewModel, Modifier.verticalScroll(rememberScrollState()))
                }
            }
        }
    }
}

/** The search, the active filters, the count and the sort, then the moves */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MovesContent(viewModel: PokedexMovesViewModel, isCompact: Boolean) {
    val navigator = LocalAppNavigator.current
    var showFiltersSheet by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    // new filters or a new order start from the first move
    val shape = viewModel.filters to viewModel.sort
    var shownShape by remember { mutableStateOf(shape) }
    LaunchedEffect(shape) {
        if (shape != shownShape) {
            shownShape = shape
            gridState.scrollToItem(0)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        SearchField(viewModel, Modifier.weight(1f))
        if (isCompact) {
            Spacer(Modifier.width(8.dp))
            FiltersButton(viewModel.filters.activeCount) { showFiltersSheet = true }
        }
    }
    Spacer(Modifier.height(8.dp))
    ActiveFilters(viewModel)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            if (viewModel.isLoading) "" else "${viewModel.moves.size} of ${viewModel.allMoves.size} moves",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        val sort = viewModel.sort
        SortChip(
            criteria = PokedexMoveSortCriterion.entries,
            criterion = sort.criterion,
            descending = sort.descending,
            isDefault = sort.isDefault,
            onSortChange = { criterion, descending -> viewModel.sort = PokedexMoveSort(criterion, descending) },
            onReset = { viewModel.sort = PokedexMoveSort() },
            resetLabel = "Reset to A → Z",
        )
    }
    Spacer(Modifier.height(8.dp))
    when {
        viewModel.isLoading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        viewModel.loadingFailed -> EmptyText("The Pokédex couldn't be loaded")
        viewModel.moves.isEmpty() -> EmptyText("No move matches the search and the filters")
        else -> LazyVerticalGridWithScrollbar(
            columns = GridCells.Adaptive(CARD_MIN_WIDTH),
            state = gridState,
            // on desktop, room for the scrollbar on the right of the cards
            contentPadding = PaddingValues(bottom = 16.dp, end = if (isCompact) 0.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(viewModel.moves, key = { it.id }) { move ->
                MoveCard(move, onClick = { navigator.push(AppRoute.PokedexMove(move.id)) })
            }
        }
    }
    if (showFiltersSheet) {
        ModalBottomSheet(onDismissRequest = { showFiltersSheet = false }) {
            FiltersPanel(viewModel, Modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp))
        }
    }
}

@Composable
private fun SearchField(viewModel: PokedexMovesViewModel, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = viewModel.query,
        onValueChange = { viewModel.query = it },
        placeholder = { Text("Search moves by name or description...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        singleLine = true,
        keyboardOptions = searchKeyboardOptions,
        keyboardActions = searchKeyboardActions(),
        shape = RoundedCornerShape(12.dp),
        trailingIcon = {
            if (viewModel.query.isNotEmpty()) {
                IconButton(onClick = { viewModel.query = "" }) {
                    Icon(painterResource(Res.drawable.close), contentDescription = "Clear the search")
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun FiltersButton(activeCount: Int, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, shape = RoundedCornerShape(12.dp), modifier = Modifier.height(56.dp)) {
        Text("Filters", fontWeight = FontWeight.Bold)
        if (activeCount > 0) {
            Spacer(Modifier.width(8.dp))
            Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("$activeCount") }
        }
    }
}

/** A chip for each active filter, removing it, and a button clearing them all */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveFilters(viewModel: PokedexMovesViewModel) {
    val filters = viewModel.filters
    val active = buildList<Pair<String, PokedexMoveFilters>> {
        filters.category?.let { add(it.label to filters.copy(category = null)) }
        filters.type?.let { add(it.displayName to filters.copy(type = null)) }
        filters.classification?.let { add(it.label to filters.copy(classification = null)) }
        filters.target?.let { add(it.label to filters.copy(target = null)) }
        filters.priority?.let { add("Priority ${priorityLabel(it)}" to filters.copy(priority = null)) }
        filters.pp?.let { add("$it PP" to filters.copy(pp = null)) }
        filters.power?.let { add("Power ${it.label}" to filters.copy(power = null)) }
        filters.accuracy?.let { add("Accuracy ${it.label}" to filters.copy(accuracy = null)) }
    }
    if (active.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        active.forEach { (label, withoutIt) ->
            InputChip(
                selected = true,
                onClick = { viewModel.filters = withoutIt },
                label = { Text(label) },
                trailingIcon = {
                    Icon(painterResource(Res.drawable.close), contentDescription = "Remove", Modifier.size(16.dp))
                },
            )
        }
        TextButton(onClick = viewModel::clearFilters) { Text("Clear") }
    }
}

/**
 * The filters, each in a section showing its value and expanding to its choices, all collapsed at first
 */
@Composable
private fun FiltersPanel(viewModel: PokedexMovesViewModel, modifier: Modifier = Modifier) {
    val expanded = remember { mutableStateMapOf<MoveFilterSection, Boolean>() }
    val filters = viewModel.filters
    Column(modifier) {
        for (section in MoveFilterSection.entries) {
            val isExpanded = expanded[section] == true
            val toggle = { expanded[section] = !isExpanded }
            when (section) {
                MoveFilterSection.CATEGORY -> FilterSection(
                    section, isExpanded, toggle, DamageClass.entries, filters.category, { it.label },
                    leadingIcon = { DamageClassImage(it, Modifier.size(18.dp)) },
                ) { viewModel.filters = filters.copy(category = it) }
                MoveFilterSection.TYPE -> FilterSection(
                    section, isExpanded, toggle, PokeType.entries.sortedBy { it.displayName }, filters.type, { it.displayName },
                    leadingIcon = { MoveTypeImage(it, Modifier.size(18.dp)) },
                ) { viewModel.filters = filters.copy(type = it) }
                MoveFilterSection.CLASSIFICATION -> FilterSection(
                    section, isExpanded, toggle, MoveClassification.entries, filters.classification, { it.label },
                ) { viewModel.filters = filters.copy(classification = it) }
                MoveFilterSection.TARGET -> FilterSection(
                    section, isExpanded, toggle, MoveTargetGroup.entries, filters.target, { it.label },
                ) { viewModel.filters = filters.copy(target = it) }
                MoveFilterSection.PRIORITY -> FilterSection(
                    section, isExpanded, toggle, viewModel.priorities, filters.priority, ::priorityLabel,
                ) { viewModel.filters = filters.copy(priority = it) }
                MoveFilterSection.PP -> FilterSection(
                    section, isExpanded, toggle, viewModel.pps, filters.pp, { it.toString() },
                ) { viewModel.filters = filters.copy(pp = it) }
                MoveFilterSection.POWER -> FilterSection(
                    section, isExpanded, toggle, MovePowerRange.entries, filters.power, { it.label },
                ) { viewModel.filters = filters.copy(power = it) }
                MoveFilterSection.ACCURACY -> FilterSection(
                    section, isExpanded, toggle, MoveAccuracyRange.entries, filters.accuracy, { it.label },
                ) { viewModel.filters = filters.copy(accuracy = it) }
            }
            HorizontalDivider()
        }
    }
}

/**
 * A filter: its title and value, expanding to the choices of an "Any" chip and a chip for each value
 *
 * @param onSelect called with the picked value, null for any
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun <T> FilterSection(
    section: MoveFilterSection,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    values: List<T>,
    selected: T?,
    label: (T) -> String,
    leadingIcon: (@Composable (T) -> Unit)? = null,
    onSelect: (T?) -> Unit,
) {
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(section.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                selected?.let(label) ?: "Any",
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected != null) FontWeight.Bold else null,
            )
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
        }
        AnimatedVisibility(isExpanded) {
            FlowRow(
                Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("Any") })
                values.forEach { value ->
                    FilterChip(
                        selected = value == selected,
                        // a second click on the selected value goes back to any
                        onClick = { onSelect(if (value == selected) null else value) },
                        label = { Text(label(value)) },
                        leadingIcon = leadingIcon?.let { icon -> { icon(value) } },
                    )
                }
            }
        }
    }
}

/** A move's name and icons, its description, then its power, accuracy, PP, priority and classifications */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoveCard(move: PokedexMove, onClick: () -> Unit) {
    val surface = MaterialTheme.colorScheme.surface
    val typeColor = move.type.color
    // stronger in dark mode, where a light tint is barely seen
    val tint = if (surface.luminance() < 0.5f) CARD_TINT_DARK else CARD_TINT_LIGHT
    MyCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = typeColor.copy(alpha = tint).compositeOver(surface)),
        border = BorderStroke(1.dp, typeColor.copy(alpha = CARD_BORDER_ALPHA)),
        elevation = CardDefaults.elevatedCardElevation(0.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    move.name.en,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                MoveTypeImage(move.type, Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                DamageClassImage(move.damageClass, Modifier.size(width = 28.dp, height = 22.dp))
            }
            Text(
                move.shortDesc ?: move.desc ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(moveFigures(move), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                if (move.priority != 0) Tag("Priority ${priorityLabel(move.priority)}", highlighted = true)
                move.classifications.forEach { Tag(it.label) }
            }
        }
    }
}

/** e.g. 120 BP · 100% · 8 PP, without power for a move having none */
private fun moveFigures(move: PokedexMove) = listOfNotNull(
    move.basePower.takeIf { it > 0 }?.let { "$it BP" },
    move.accuracy?.let { "$it%" } ?: "Never misses",
    "${move.pp} PP",
).joinToString(" · ")

/** e.g. +2, -7 */
private fun priorityLabel(priority: Int) = if (priority > 0) "+$priority" else priority.toString()

@Composable
private fun Tag(text: String, highlighted: Boolean = false) {
    val color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
internal fun DamageClassImage(damageClass: DamageClass, modifier: Modifier = Modifier) {
    val resource = when (damageClass) {
        DamageClass.PHYSICAL -> Res.drawable.move_physical
        DamageClass.SPECIAL -> Res.drawable.move_special
        DamageClass.STATUS -> Res.drawable.move_status
    }
    Image(painterResource(resource), contentDescription = damageClass.label, contentScale = ContentScale.Fit, modifier = modifier)
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(32.dp),
    )
}
