package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.check
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.PokedexSpecies
import com.tambapps.pokemon.alakastats.ui.composables.LazyVerticalGridWithScrollbar
import com.tambapps.pokemon.alakastats.ui.navigation.AppRoute
import com.tambapps.pokemon.alakastats.ui.navigation.LocalAppNavigator
import org.jetbrains.compose.resources.painterResource

private val POKEMON_CARD_MIN_WIDTH = 300.dp

/**
 * Pokémon of a sheet (e.g. the ones learning a move) as the cards of the Pokémon's list, opening their sheet, after
 * items spanning the whole width (e.g. the filters, and on mobile the sheet itself). On desktop, with a scrollbar
 *
 * @param emptyText what tells there's no Pokémon to show
 * @param tag a tag of a Pokémon's card, e.g. STAB
 * @param header the items before the Pokémon, each spanning the whole width
 */
@Composable
internal fun PokedexPokemonGrid(
    pokemons: List<PokedexSpecies>,
    showSpeed: Boolean,
    state: LazyGridState,
    isCompact: Boolean,
    emptyText: String,
    modifier: Modifier = Modifier,
    tag: (PokedexSpecies) -> String? = { null },
    header: LazyGridScope.() -> Unit,
) {
    val navigator = LocalAppNavigator.current
    val content: LazyGridScope.() -> Unit = {
        header()
        if (pokemons.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { CenteredText(emptyText) }
        }
        items(pokemons, key = { it.id }) { pokemon ->
            PokemonListCard(
                pokemon,
                showSpeed = showSpeed,
                spriteSize = if (isCompact) 72.dp else 88.dp,
                onClick = { navigator.push(AppRoute.PokedexPokemon(pokemon.id)) },
                tag = tag(pokemon),
            )
        }
    }
    if (isCompact) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(POKEMON_CARD_MIN_WIDTH),
            state = state,
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier.fillMaxSize(),
            content = content,
        )
    } else {
        LazyVerticalGridWithScrollbar(
            columns = GridCells.Adaptive(POKEMON_CARD_MIN_WIDTH),
            state = state,
            // room for the scrollbar on the right of the cards
            contentPadding = PaddingValues(bottom = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier.fillMaxSize(),
            content = content,
        )
    }
}

/** A filter chip turned on and off, checked when on */
@Composable
internal fun ToggleChip(label: String, selected: Boolean, onChange: (Boolean) -> Unit) {
    FilterChip(
        selected = selected,
        onClick = { onChange(!selected) },
        label = { Text(label, maxLines = 1) },
        leadingIcon = if (selected) {
            { Icon(painterResource(Res.drawable.check), contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
        } else null,
    )
}

@Composable
internal fun CenteredText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(32.dp),
    )
}
