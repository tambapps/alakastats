package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.ItemName
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.Player
import com.tambapps.pokemon.alakastats.ui.service.FacingDirection
import com.tambapps.pokemon.alakastats.ui.service.ItemImage
import com.tambapps.pokemon.alakastats.ui.service.PokemonSprite
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.pokepaste.parser.PokePaste
import com.tambapps.pokemon.util.MegaUtils.toMega

/**
 * The player's team preview with, on desktop, the items known from their OTS or their Mega Evolution
 */
@Composable
fun PokemonTeamPreview(
    player: Player,
    fillWidth: Boolean = false,
    modifier: Modifier = Modifier,
    facingDirection: FacingDirection = FacingDirection.LEFT,
    ) {
    val names = player.teamPreview.pokemons.map { it.name }
    val items = remember(player) {
        names.map { name ->
            player.ots?.pokemons?.find { it.name.baseMatches(name) }?.item
                ?: player.megaEvolution?.takeIf { it.pokemon.baseMatches(name) }?.item
        }
    }
    PokemonTeamPreview(names, items, modifier, fillWidth, facingDirection)
}

@Composable
fun PokemonTeamPreview(
    pokemons: List<PokemonName>,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = false,
    facingDirection: FacingDirection = FacingDirection.LEFT) {
    Row(
        modifier = modifier.heightIn(max = if (LocalIsCompact.current) 60.dp else 80.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (pokemon in pokemons) {
            PokemonSprite(
                name = pokemon,
                modifier = if (fillWidth) Modifier.weight(1f) else Modifier,
                facingDirection = facingDirection,
            )
        }
    }
}

/**
 * The paste's Pokémon with, on desktop, their item at the bottom right of their sprite.
 * Takes a paste rather than a list of Pokémon, whose overload would clash on the JVM with the list of names one
 */
@Composable
fun PokemonTeamPreview(
    pokePaste: PokePaste,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = false,
    facingDirection: FacingDirection = FacingDirection.LEFT) {
    PokemonTeamPreview(pokePaste.pokemons.map { it.name }, pokePaste.pokemons.map { it.item }, modifier, fillWidth, facingDirection)
}

/**
 * The Pokémon with, on desktop, their item at the bottom right of their sprite. A Pokémon holding its Mega Stone
 * shows as its Mega Evolution instead, without the stone, on a phone too
 *
 * @param items the item of each of the pokemons, in the same order. A missing or null one isn't shown
 */
@Composable
fun PokemonTeamPreview(
    pokemons: List<PokemonName>,
    items: List<ItemName?>,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = false,
    facingDirection: FacingDirection = FacingDirection.LEFT) {
    val shown = remember(pokemons, items) { withMegaForms(pokemons, items) }
    if (LocalIsCompact.current) {
        // no room for the items on a phone
        PokemonTeamPreview(shown.map { it.first }, modifier, fillWidth, facingDirection)
        return
    }
    Row(
        modifier = modifier.heightIn(max = 80.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for ((name, item) in shown) {
            // centered in its share of the width, so that the item stays on the sprite
            Box(if (fillWidth) Modifier.weight(1f) else Modifier, contentAlignment = Alignment.Center) {
                PokemonSpriteWithItem(name, item, facingDirection)
            }
        }
    }
}

/**
 * Each Pokémon with its item, except a Pokémon holding its Mega Stone: its Mega Evolution, without item
 */
internal fun withMegaForms(pokemons: List<PokemonName>, items: List<ItemName?>): List<Pair<PokemonName, ItemName?>> =
    pokemons.mapIndexed { index, name ->
        val item = items.getOrNull(index)
        val mega = item?.let { name.toMega(it) }
        if (mega != null) mega to null else name to item
    }

@Composable
private fun PokemonSpriteWithItem(name: PokemonName, item: ItemName?, facingDirection: FacingDirection) {
    Box(Modifier.size(80.dp)) {
        PokemonSprite(name = name, modifier = Modifier.fillMaxSize(), facingDirection = facingDirection)
        item?.let { item ->
            // overlapping the sprite a bit
            ItemImage(item, Modifier.align(Alignment.BottomEnd).size(32.dp))
        }
    }
}
