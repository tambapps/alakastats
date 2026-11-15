package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.check
import alakastats.composeapp.generated.resources.close
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.Stat
import com.tambapps.pokemon.alakastats.domain.model.PokedexSpecies
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton
import com.tambapps.pokemon.alakastats.ui.composables.LazyVerticalGridWithScrollbar
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.SortChip
import com.tambapps.pokemon.alakastats.ui.composables.TypeBadge
import com.tambapps.pokemon.alakastats.ui.model.PokedexPokemonSort
import com.tambapps.pokemon.alakastats.ui.model.PokedexPokemonSortCriterion
import com.tambapps.pokemon.alakastats.ui.navigation.AppRoute
import com.tambapps.pokemon.alakastats.ui.navigation.LocalAppNavigator
import com.tambapps.pokemon.alakastats.ui.service.FacingDirection
import com.tambapps.pokemon.alakastats.ui.service.PokemonSprite
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.theme.color
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

// the share of the primary type's color in a card's background, and in its border
private const val CARD_TINT_LIGHT = 0.16f
private const val CARD_TINT_DARK = 0.24f
private const val CARD_BORDER_ALPHA = 0.45f
private val CARD_MIN_WIDTH = 340.dp

/**
 * The Pokémon of the Pokédex as cards in the color of their type, searched and sorted by name. A card opens the
 * Pokémon's sheet
 */
@Composable
fun PokedexPokemonListScreen() {
    val viewModel = koinViewModel<PokedexPokemonListViewModel>()
    val navigator = LocalAppNavigator.current
    val isCompact = LocalIsCompact.current
    LaunchedEffect(Unit) { viewModel.load() }
    val gridState = rememberLazyGridState()
    // a new order starts from its first Pokémon
    var shownSort by remember { mutableStateOf(viewModel.sort) }
    LaunchedEffect(viewModel.sort) {
        if (viewModel.sort != shownSort) {
            shownSort = viewModel.sort
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
                "Pokémon",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier.fillMaxWidth().widthIn(max = 1200.dp).align(Alignment.CenterHorizontally)
                .padding(horizontal = 8.dp)
        ) {
            SearchField(viewModel)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (viewModel.isLoading) "" else "${viewModel.pokemons.size} Pokémon",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                MegasChip(viewModel, isCompact)
                Spacer(Modifier.width(8.dp))
                PokemonSortChip(viewModel)
            }
            Spacer(Modifier.height(8.dp))
            when {
                viewModel.isLoading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                viewModel.loadingFailed -> EmptyText("The Pokédex couldn't be loaded")
                viewModel.pokemons.isEmpty() -> EmptyText("No Pokémon matches \"${viewModel.query}\"")
                else -> LazyVerticalGridWithScrollbar(
                    columns = GridCells.Adaptive(CARD_MIN_WIDTH),
                    state = gridState,
                    // on desktop, room for the scrollbar on the right of the cards
                    contentPadding = PaddingValues(bottom = 16.dp, end = if (isCompact) 0.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(viewModel.pokemons, key = { it.id }) { pokemon ->
                        PokemonListCard(
                            pokemon,
                            showSpeed = viewModel.sort.criterion == PokedexPokemonSortCriterion.SPEED,
                            spriteSize = if (isCompact) 72.dp else 96.dp,
                            onClick = { navigator.push(AppRoute.PokedexPokemon(pokemon.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(viewModel: PokedexPokemonListViewModel) {
    OutlinedTextField(
        value = viewModel.query,
        onValueChange = { viewModel.query = it },
        placeholder = { Text("Search Pokémon...") },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        trailingIcon = {
            if (viewModel.query.isNotEmpty()) {
                IconButton(onClick = { viewModel.query = "" }) {
                    Icon(painterResource(Res.drawable.close), contentDescription = "Clear the search")
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Whether the Megas are listed, with their Pokémon */
@Composable
private fun MegasChip(viewModel: PokedexPokemonListViewModel, isCompact: Boolean) {
    FilterChip(
        selected = viewModel.includeMegas,
        onClick = { viewModel.includeMegas = !viewModel.includeMegas },
        // shorter on mobile, where the row has less room
        label = { Text(if (isCompact) "Megas" else "Include Megas", maxLines = 1) },
        leadingIcon = if (viewModel.includeMegas) {
            { Icon(painterResource(Res.drawable.check), contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
        } else null,
    )
}

@Composable
private fun PokemonSortChip(viewModel: PokedexPokemonListViewModel) {
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

/**
 * A Pokémon's sprite, name and types, on the color of its primary type, then its base stat total or, when the Pokémon
 * are sorted by speed, its speed
 */
@Composable
private fun PokemonListCard(pokemon: PokedexSpecies, showSpeed: Boolean, spriteSize: Dp, onClick: () -> Unit) {
    val surface = MaterialTheme.colorScheme.surface
    val typeColor = pokemon.types.first().color
    // stronger in dark mode, where a light tint is barely seen
    val tint = if (surface.luminance() < 0.5f) CARD_TINT_DARK else CARD_TINT_LIGHT
    MyCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = typeColor.copy(alpha = tint).compositeOver(surface)),
        border = BorderStroke(1.dp, typeColor.copy(alpha = CARD_BORDER_ALPHA)),
        elevation = CardDefaults.elevatedCardElevation(0.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // facing the text
            PokemonSprite(pokemon.pokemonName, Modifier.size(spriteSize), FacingDirection.RIGHT)
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(pokemon.name.en, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pokemon.types.forEach { TypeBadge(it) }
                }
                if (showSpeed) {
                    SpeedStats(pokemon.baseStats)
                } else {
                    Text(
                        "Base stat total ${pokemon.baseStats.total}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** The level 50 speeds of a Pokémon, like on its sheet: max, neutral, without stat points, min */
@Composable
private fun SpeedStats(baseStats: PokeStats) {
    val speeds = remember(baseStats) { levelFiftyStats(baseStats, Stat.SPEED) }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SpeedValue("Max", speeds.max)
        SpeedValue("Neu", speeds.neutral)
        SpeedValue("0 SP", speeds.noStatPoints)
        SpeedValue("Min", speeds.min)
    }
}

@Composable
private fun SpeedValue(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(value.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

private val PokeStats.total: Int
    get() = hp + attack + defense + specialAttack + specialDefense + speed
