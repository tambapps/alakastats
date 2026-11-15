package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.arrow_forward
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.alakastats.domain.model.PokedexSpecies
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton
import com.tambapps.pokemon.alakastats.ui.composables.LazyVerticalGridWithScrollbar
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.TypeBadge
import com.tambapps.pokemon.alakastats.ui.navigation.AppRoute
import com.tambapps.pokemon.alakastats.ui.navigation.LocalAppNavigator
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
                NameOrderChip(viewModel)
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
                    // on desktop, room for the scrollbar on the right of the cards
                    contentPadding = PaddingValues(bottom = 16.dp, end = if (isCompact) 0.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(viewModel.pokemons, key = { it.id }) { pokemon ->
                        PokemonListCard(
                            pokemon,
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

/** The only order for now, by name: a click reverses it */
@Composable
private fun NameOrderChip(viewModel: PokedexPokemonListViewModel) {
    val label = if (viewModel.descending) "Name · Z → A" else "Name · A → Z"
    AssistChip(
        onClick = viewModel::toggleOrder,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                painterResource(Res.drawable.arrow_forward),
                contentDescription = null,
                // pointing down for the descending order
                modifier = Modifier.size(18.dp).rotate(if (viewModel.descending) 90f else -90f),
            )
        },
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

/** A Pokémon's sprite, name and types, on the color of its primary type */
@Composable
private fun PokemonListCard(pokemon: PokedexSpecies, spriteSize: Dp, onClick: () -> Unit) {
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
            PokemonSprite(pokemon.pokemonName, Modifier.size(spriteSize))
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(pokemon.name.en, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pokemon.types.forEach { TypeBadge(it) }
                }
                Text(
                    "Base stat total ${pokemon.baseStats.total}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val PokeStats.total: Int
    get() = hp + attack + defense + specialAttack + specialDefense + speed
