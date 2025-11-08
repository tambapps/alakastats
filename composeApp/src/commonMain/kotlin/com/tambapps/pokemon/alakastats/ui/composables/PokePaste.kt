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
import androidx.compose.material3.MaterialTheme
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
import com.tambapps.pokemon.PokemonNormalizer
import com.tambapps.pokemon.Stat
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.pokepaste.parser.PokePaste

@Composable
fun Pokepaste(
    pokePaste: PokePaste,
    pokemonImageService: PokemonImageService,
    modifier: Modifier = Modifier,
    pokemonNotes: Map<Pokemon, String>? = null
) {
    val isCompact = LocalIsCompact.current
    if (isCompact) {
        VerticalPokepaste(pokePaste, pokemonImageService, modifier, pokemonNotes)
    } else {
        DesktopPokepaste(pokePaste, pokemonImageService, modifier, pokemonNotes)
    }
}


@Composable
fun VerticalPokepaste(
    pokePaste: PokePaste,
    pokemonImageService: PokemonImageService,
    modifier: Modifier = Modifier,
    pokemonNotes: Map<Pokemon, String>? = null) {
    Column(modifier = modifier) {
        val space = 32.dp
        Spacer(Modifier.height(space))
        for (pokemon in pokePaste.pokemons) {
            val notes = pokemonNotes?.get(pokemon)
            PokepastePokemon(pokePaste.isOts, pokemon, pokemonImageService, Modifier.fillMaxWidth(), notes)
            Spacer(Modifier.height(space))
        }

    }
}

@Composable
private fun DesktopPokepaste(pokePaste: PokePaste, pokemonImageService: PokemonImageService, modifier: Modifier, pokemonNotes: Map<Pokemon, String>? = null) {
    Column(modifier = modifier) {
        for (pokemonBlock in pokePaste.pokemons.chunked(3)) {
            DesktopPokemonRow(pokePaste.isOts, pokemonBlock, pokemonImageService, pokemonNotes)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DesktopPokemonRow(
    isOts: Boolean,
    pokemons: List<Pokemon>,
    pokemonImageService: PokemonImageService,
    pokemonNotes: Map<Pokemon, String>?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        pokemons.forEach { pokemon ->
            val notes = pokemonNotes?.get(pokemon)
            PokepastePokemon(isOts, pokemon, pokemonImageService, Modifier.weight(1f), notes)
        }
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
                    Text(prettyMove, textAlign = TextAlign.Start, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                }
            }
            if (index < pokemon.moves.lastIndex) {
                Spacer(Modifier.height(8.dp))
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
        Text(txt, color = textColor, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        val statValueStyle = MaterialTheme.typography.titleMedium
        Text(evs[stat].toString(), color = textColor, textAlign = TextAlign.Center, style = statValueStyle)
        Text(ivs[stat].toString(), color = textColor, textAlign = TextAlign.Center, style = statValueStyle)
    }
}
@Composable
fun PokepastePokemon(
    isOts: Boolean,
    pokemon: Pokemon,
    pokemonImageService: PokemonImageService,
    modifier: Modifier = Modifier,
    notes: String? = null
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        MyCard(
            modifier = Modifier.fillMaxWidth(0.9f),
            onClick = {},
            gradientBackground = true,
            enabled = false
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                pokemon.teraType?.let {
                    pokemonImageService.TeraTypeImage(it, modifier = Modifier.size(45.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text(pokemon.name.pretty, style = MaterialTheme.typography.headlineLarge)
                if (notes != null) {
                    // TODO handle editing mode
                    Text(notes, style = MaterialTheme.typography.bodyMedium)
                }
                if (!isOts) {
                    // only want margin if above element is not headline text because headline already has a lot of margin
                    if (notes != null) Spacer(Modifier.height(8.dp))
                    PokemonStatsRow(pokemon, Modifier.fillMaxWidth())
                }
                if (notes != null || !isOts) Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    pokemon.item?.let {
                        pokemonImageService.ItemImage(it, modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text((pokemon.item ?: "<no item>") + " | " + (pokemon.ability ?: "<no ability>"), style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(16.dp))
                PokemonMoves(pokemon, pokemonImageService)
            }
        }
        pokemonImageService.PokemonArtwork(
            modifier = Modifier.align(Alignment.BottomEnd).height(if (LocalIsCompact.current) 175.dp else 200.dp)
                .offset(y = 16.dp),
            name = pokemon.name
        )
    }

    // TODO delete below
    if (true) return
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