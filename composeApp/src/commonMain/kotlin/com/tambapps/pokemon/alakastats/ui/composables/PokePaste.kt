package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.Stat
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.util.PokemonNormalizer
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
        val space = 16.dp
        Spacer(Modifier.height(space))
        for (pokemon in pokePaste.pokemons) {
            Pokemon(pokePaste.isOts, pokemon, pokemonImageService, Modifier.fillMaxWidth())
            Spacer(Modifier.height(space))
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
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        pokemons.forEach { pokemon ->
            Pokemon(isOts, pokemon, pokemonImageService, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Pokemon(isOts: Boolean, pokemon: Pokemon, pokemonImageService: PokemonImageService, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (weightA, weightB) = if (!isOts) Pair(0.35f, 0.65f) else Pair(0.45f, 0.55f)
        PokemonView(pokemon, pokemonImageService, Modifier.weight(weightA))
        Spacer(Modifier.width(8.dp))
        PokemonDetails(isOts, pokemon, pokemonImageService, Modifier.weight(weightB))
    }
}

@Composable
private fun PokemonDetails(isOts: Boolean, pokemon: Pokemon, pokemonImageService: PokemonImageService, modifier: Modifier = Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isOts) {
            PokemonStatsRow(pokemon)
        }
        PokemonMoves(pokemon, pokemonImageService)
    }
}

@Composable
private fun PokemonStatsRow(pokemon: Pokemon, modifier: Modifier = Modifier) {
    Row(modifier) {
        for (stat in listOf(Stat.HP, Stat.ATTACK, Stat.DEFENSE, Stat.SPECIAL_ATTACK, Stat.SPECIAL_DEFENSE, Stat.SPEED)) {
            val modifier = if (LocalIsCompact.current) Modifier.weight(1f) else Modifier.padding(horizontal = 4.dp)
            PokemonStatColumn(pokemon, stat, pokemon.ivs, pokemon.evs, modifier)
        }
    }
}
@Composable
private fun PokemonMoves(pokemon: Pokemon, pokemonImageService: PokemonImageService, modifier: Modifier = Modifier) {
    Column(modifier) {
        pokemon.moves.forEachIndexed { index, move ->
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val iconModifier = Modifier.size(32.dp)
                pokemonImageService.MoveSpecImages(move, iconModifier)
                Spacer(Modifier.width(8.dp))
                val prettyMove = PokemonNormalizer.pretty(move)
                Tooltip(prettyMove) {
                    Text(prettyMove, textAlign = TextAlign.Start, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (index < pokemon.moves.lastIndex) {
                Spacer(Modifier.height(4.dp))
            }
         }
    }
}

@Composable
private fun PokemonStatColumn(
    pokemon: Pokemon,
    stat: Stat,
    ivs: PokeStats,
    evs: PokeStats,
    modifier: Modifier = Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val txt = when(stat) {
            Stat.ATTACK -> "Atk"
            Stat.DEFENSE -> "Def"
            Stat.SPECIAL_ATTACK -> "SpA"
            Stat.SPECIAL_DEFENSE -> "SpD"
            Stat.SPEED -> "Spe"
            Stat.HP -> "HP"
        }
        pokemon.nature?.bonusStat
        val textColor = when {
            pokemon.nature?.bonusStat == stat -> Color.Red
            pokemon.nature?.malusStat == stat -> Color.Cyan
            else -> Color.Unspecified
        }
        Text(txt, color = textColor, textAlign = TextAlign.Center)
        Text(evs[stat].toString(), color = textColor, textAlign = TextAlign.Center)
        Text(ivs[stat].toString(), color = textColor, textAlign = TextAlign.Center)
    }
}
@Composable
private fun PokemonView(pokemon: Pokemon, pokemonImageService: PokemonImageService, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val scale = 0.75f
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
            contentAlignment = Alignment.Center
        ) {
            pokemonImageService.PokemonArtwork(pokemon.name)
        }
        val offset = 16.dp
        pokemon.teraType?.let {
            pokemonImageService.TeraTypeImage(it, modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 0.dp, y = -offset)
                .size(50.dp)
            )
        }
        // Bottom-right badge
        pokemon.item?.let {
            pokemonImageService.ItemImage(it, modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(40.dp)
                .offset(x = 0.dp, y = offset)
            )
        }
    }

}