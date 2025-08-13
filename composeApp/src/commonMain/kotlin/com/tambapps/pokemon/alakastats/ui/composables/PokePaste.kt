package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.pokepaste.parser.PokePaste

@Composable
fun Pokepaste(
    pokePaste: PokePaste,
    pokemonImageService: PokemonImageService,
    modifier: Modifier = Modifier
) {
    val isCompact = LocalIsCompact.current
    if (isCompact) {
        MobilePokepaste(pokePaste, pokemonImageService, modifier)
    } else {
        DesktopPokepaste(pokePaste, pokemonImageService, modifier)
    }
}


@Composable
private fun MobilePokepaste(
    pokePaste: PokePaste,
    pokemonImageService: PokemonImageService,
    modifier: Modifier) {
    Column(modifier = modifier) {
        for (pokemon in pokePaste.pokemons) {
            Pokemon(pokemon, pokemonImageService)
        }

    }
}

@Composable
private fun DesktopPokepaste(pokePaste: PokePaste, pokemonImageService: PokemonImageService, modifier: Modifier) {
    Column(modifier = modifier) {
        val firstRow = pokePaste.pokemons.take(3)
        DesktopPokemonRow(firstRow, pokemonImageService)
        if (pokePaste.pokemons.size > 3) {
            val secondRow = pokePaste.pokemons.subList(3, pokePaste.pokemons.size)
            DesktopPokemonRow(secondRow, pokemonImageService)
        }
    }
}

@Composable
private fun DesktopPokemonRow(pokemons: List<Pokemon>, pokemonImageService: PokemonImageService) {
    Row {
        pokemons.forEach { pokemon ->
            Pokemon(pokemon, pokemonImageService, Modifier)
        }
    }
}

@Composable
private fun Pokemon(pokemon: Pokemon, pokemonImageService: PokemonImageService, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val scale = 1f
        Box(
            modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            contentAlignment = Alignment.Center
        ) {
            pokemonImageService.PokemonArtwork(pokemon.name)
        }
        val iconSize = 64.dp
        val offset = 8.dp
        pokemon.teraType?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 0.dp, y = offset)
                    .size(iconSize)
            ) {
                pokemonImageService.TeraTypeImage(it)
            }
        }
        // Bottom-right badge
        pokemon.item?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(iconSize)
                    .offset(x = 0.dp, y = 0.dp)
            ) {
                pokemonImageService.ItemImage(it)
            }
        }
    }
}