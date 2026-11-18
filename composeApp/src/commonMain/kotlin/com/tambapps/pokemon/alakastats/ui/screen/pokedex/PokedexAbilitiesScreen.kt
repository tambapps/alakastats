package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.close
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton
import com.tambapps.pokemon.alakastats.ui.composables.LazyVerticalGridWithScrollbar
import com.tambapps.pokemon.alakastats.ui.composables.searchKeyboardActions
import com.tambapps.pokemon.alakastats.ui.composables.searchKeyboardOptions
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.SortChip
import com.tambapps.pokemon.alakastats.ui.composables.elevatedCardGradientColors
import com.tambapps.pokemon.alakastats.ui.navigation.AppRoute
import com.tambapps.pokemon.alakastats.ui.navigation.LocalAppNavigator
import com.tambapps.pokemon.alakastats.ui.service.FacingDirection
import com.tambapps.pokemon.alakastats.ui.service.PokemonSprite
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

private val CARD_MIN_WIDTH = 340.dp
// the Pokémon of an ability shown on its card, the others counted
private const val CARD_SPRITE_COUNT = 6

/**
 * The abilities of the Pokédex as cards with what they do and who has them, searched by name or description, filtered
 * and sorted. A card opens the ability's sheet
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PokedexAbilitiesScreen() {
    val viewModel = koinViewModel<PokedexAbilitiesViewModel>()
    val navigator = LocalAppNavigator.current
    val isCompact = LocalIsCompact.current
    LaunchedEffect(Unit) { viewModel.load() }
    val gridState = rememberLazyGridState()
    // new filters or a new order start from the first ability
    val shape = viewModel.sort to viewModel.megaOnly
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
                "Abilities",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier.fillMaxWidth().widthIn(max = 1400.dp).align(Alignment.CenterHorizontally)
                .padding(horizontal = 8.dp)
        ) {
            OutlinedTextField(
                value = viewModel.query,
                onValueChange = { viewModel.query = it },
                placeholder = { Text("Search abilities by name or description...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                ToggleChip("Mega abilities", viewModel.megaOnly) { viewModel.megaOnly = it }
                val sort = viewModel.sort
                SortChip(
                    criteria = PokedexAbilitySortCriterion.entries,
                    criterion = sort.criterion,
                    descending = sort.descending,
                    isDefault = sort.isDefault,
                    onSortChange = { criterion, descending -> viewModel.sort = PokedexAbilitySort(criterion, descending) },
                    onReset = { viewModel.sort = PokedexAbilitySort() },
                    resetLabel = "Reset to A → Z",
                )
            }
            Text(
                if (viewModel.isLoading) "" else "${viewModel.abilities.size} of ${viewModel.all.size} abilities",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            when {
                viewModel.isLoading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                viewModel.loadingFailed -> CenteredText("The Pokédex couldn't be loaded")
                viewModel.abilities.isEmpty() -> CenteredText("No ability matches the search and the filters")
                else -> LazyVerticalGridWithScrollbar(
                    columns = GridCells.Adaptive(CARD_MIN_WIDTH),
                    state = gridState,
                    // on desktop, room for the scrollbar on the right of the cards
                    contentPadding = PaddingValues(bottom = 16.dp, end = if (isCompact) 0.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(viewModel.abilities, key = { it.ability.id }) { holders ->
                        AbilityCard(holders, onClick = { navigator.push(AppRoute.PokedexAbility(holders.ability.id)) })
                    }
                }
            }
        }
    }
}

/** An ability's name and tags, what it does, then the sprites of the Pokémon having it and their count */
@Composable
private fun AbilityCard(holders: AbilityHolders, onClick: () -> Unit) {
    val ability = holders.ability
    MyCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        gradientBackgroundColors = elevatedCardGradientColors,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    ability.name.en,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f, fill = false),
                )
                AbilityTags(holders)
            }
            Text(
                ability.shortDesc ?: ability.desc ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            HoldersPreview(holders)
        }
    }
}

/** The tag of an ability of Megas */
@Composable
internal fun AbilityTags(holders: AbilityHolders) {
    if (holders.isMegaAbility) AbilityTag("Mega", MaterialTheme.colorScheme.tertiary)
}

@Composable
private fun AbilityTag(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** The first Pokémon having the ability as overlapping sprites, then how many have it */
@Composable
private fun HoldersPreview(holders: AbilityHolders) {
    val shown = holders.shown
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            shown.take(CARD_SPRITE_COUNT).forEachIndexed { index, pokemon ->
                PokemonSprite(
                    pokemon.pokemonName,
                    Modifier.offset(x = (index * 26).dp).size(36.dp),
                    FacingDirection.RIGHT,
                )
            }
            // the width the overlapping sprites take
            Spacer(Modifier.width(26.dp * (minOf(shown.size, CARD_SPRITE_COUNT).coerceAtLeast(1) - 1) + 36.dp))
        }
        Spacer(Modifier.width(8.dp))
        val count = shown.size
        Text(
            when {
                count == 0 -> "No Pokémon"
                holders.isMegaAbility -> "$count Mega${if (count > 1) "s" else ""}"
                else -> "$count Pokémon"
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
