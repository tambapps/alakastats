package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.SnackBar
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.SortChip
import com.tambapps.pokemon.alakastats.ui.composables.TypeFilterChip
import com.tambapps.pokemon.alakastats.ui.composables.elevatedCardGradientColors
import com.tambapps.pokemon.alakastats.ui.model.PokedexPokemonSort
import com.tambapps.pokemon.alakastats.ui.model.PokedexPokemonSortCriterion
import com.tambapps.pokemon.alakastats.ui.navigation.LocalAppNavigator
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import org.koin.compose.viewmodel.koinViewModel

private val SUMMARY_WIDTH = 400.dp

/**
 * The sheet of an ability: what it does, then the Pokémon having it, which open their sheet. On desktop, the ability
 * is on the left of its Pokémon
 *
 * @param abilityId the ability's id in the Pokédex, e.g. intimidate
 */
@Composable
fun PokedexAbilityScreen(abilityId: String) {
    val viewModel = koinViewModel<PokedexAbilityViewModel>()
    val navigator = LocalAppNavigator.current
    val snackBar = LocalSnackBar.current
    val isCompact = LocalIsCompact.current
    LaunchedEffect(abilityId) { viewModel.load(abilityId) }
    // an ability can come from an URL, so it may not exist
    if (viewModel.isUnknown) {
        LaunchedEffect(abilityId) {
            snackBar.show("Unknown ability: $abilityId", SnackBar.Severity.ERROR)
            navigator.pop()
        }
    }
    val gridState = rememberLazyGridState()
    // new filters or a new order start from the first Pokémon
    val shape = Triple(viewModel.sort, viewModel.type, viewModel.includeMegas)
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
                "Ability",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Spacer(Modifier.height(12.dp))
        val holders = viewModel.holders
        when {
            viewModel.isLoading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            viewModel.loadingFailed -> CenteredText("The Pokédex couldn't be loaded")
            holders == null -> Unit
            // one scroll for the ability and its Pokémon, the screen being too narrow to put them side by side
            isCompact -> HoldersGrid(viewModel, gridState, isCompact = true, Modifier.padding(horizontal = 8.dp)) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        AbilitySummary(holders)
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
            else -> Row(
                Modifier.fillMaxSize().widthIn(max = 1600.dp).align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(Modifier.width(SUMMARY_WIDTH).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    AbilitySummary(holders)
                }
                HoldersGrid(viewModel, gridState, isCompact = false, Modifier.weight(1f))
            }
        }
    }
}

/**
 * The Pokémon having the ability: a title with their count, the filters and the sort, then their cards
 *
 * @param header items before the title (the ability, on mobile)
 */
@Composable
private fun HoldersGrid(
    viewModel: PokedexAbilityViewModel,
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
        emptyText = "No Pokémon matches the filters",
        modifier = modifier,
    ) {
        header()
        item(span = { GridItemSpan(maxLineSpan) }) { HoldersControls(viewModel) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HoldersControls(viewModel: PokedexAbilityViewModel) {
    val holders = viewModel.holders ?: return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            if (holders.isMegaAbility) "Had by ${holders.megas.size} Mega${if (holders.megas.size > 1) "s" else ""}"
            else "Had by ${holders.pokemons.size} Pokémon",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            TypeFilterChip(viewModel.type, onTypeChange = { viewModel.type = it }, types = viewModel.types)
            // the Megas are always listed for an ability of Megas
            if (!holders.isMegaAbility && holders.megas.isNotEmpty()) {
                ToggleChip("Include Megas", viewModel.includeMegas) { viewModel.includeMegas = it }
            }
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
    }
}

/** The ability's card: its name and tags, what it does in short, then in detail */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AbilitySummary(holders: AbilityHolders) {
    val ability = holders.ability
    MyCard(modifier = Modifier.fillMaxWidth(), gradientBackgroundColors = elevatedCardGradientColors) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(ability.name.en, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AbilityTags(holders)
            }
            ability.shortDesc?.let {
                Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            }
            // the detail, when it tells more than the short description
            val desc = ability.desc
            if (desc != null && desc != ability.shortDesc) {
                HorizontalDivider()
                Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
