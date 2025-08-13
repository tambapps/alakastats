package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.Stat
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
            Pokemon(pokePaste.isOts, pokemon, pokemonImageService, Modifier.fillMaxWidth())
        }

    }
}

@Composable
private fun DesktopPokepaste(pokePaste: PokePaste, pokemonImageService: PokemonImageService, modifier: Modifier) {
    Column(modifier = modifier) {
        val firstRow = pokePaste.pokemons.take(3)
        DesktopPokemonRow(pokePaste.isOts, firstRow, pokemonImageService)
        if (pokePaste.pokemons.size > 3) {
            val secondRow = pokePaste.pokemons.subList(3, pokePaste.pokemons.size)
            DesktopPokemonRow(pokePaste.isOts, secondRow, pokemonImageService)
        }
    }
}

@Composable
private fun DesktopPokemonRow(isOts: Boolean, pokemons: List<Pokemon>, pokemonImageService: PokemonImageService) {
    Row {
        pokemons.forEach { pokemon ->
            Pokemon(isOts, pokemon, pokemonImageService, Modifier)
        }
    }
}

@Composable
private fun Pokemon(isOts: Boolean, pokemon: Pokemon, pokemonImageService: PokemonImageService, modifier: Modifier = Modifier) {
   if (isOts) {
       PokemonView(pokemon, pokemonImageService, modifier)
   } else {
       Row(modifier) {
           PokemonView(pokemon, pokemonImageService, Modifier.weight(1f))
           PokemonDetails(pokemon, pokemonImageService, Modifier.weight(1f))
       }
   }
}

@Composable
private fun PokemonDetails(pokemon: Pokemon, pokemonImageService: PokemonImageService, modifier: Modifier = Modifier) {
    Column(modifier) {
        PokemonStatsRow(pokemon)
    }
}


@Composable
private fun PokemonStatsRow(pokemon: Pokemon, modifier: Modifier = Modifier) {
    Row(modifier) {
        for (stat in listOf(Stat.HP, Stat.ATTACK, Stat.DEFENSE, Stat.SPECIAL_ATTACK, Stat.SPECIAL_DEFENSE, Stat.SPEED)) {
            PokemonStatColumn(stat, pokemon.ivs, pokemon.evs)
        }
    }
}

@Composable
private fun PokemonStatColumn(
    stat: Stat,
    ivs: PokeStats,
    evs: PokeStats,
    modifier: Modifier = Modifier) {
    Column(modifier) {
        Text("Atk")
        Text(evs[stat].toString())
        Text(ivs[stat].toString())
    }
}
@Composable
private fun PokemonView(pokemon: Pokemon, pokemonImageService: PokemonImageService, modifier: Modifier = Modifier) {
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