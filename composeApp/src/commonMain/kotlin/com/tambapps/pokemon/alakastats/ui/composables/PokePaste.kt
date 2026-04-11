package com.tambapps.pokemon.alakastats.ui.composables

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.mega
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.Mechanic
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.Stat
import com.tambapps.pokemon.alakastats.PlatformType
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.domain.model.PokemonData
import com.tambapps.pokemon.alakastats.platform
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.theme.isDarkThemeEnabled
import com.tambapps.pokemon.alakastats.ui.viewmodels.PokepasteEditingViewModel
import com.tambapps.pokemon.pokepaste.parser.PokePaste
import com.tambapps.pokemon.util.MegaUtils
import org.jetbrains.compose.resources.painterResource

@Composable
fun Pokepaste(
    pokePaste: PokePaste,
    pokemonImageService: PokemonImageService,
    format: Format,
    modifier: Modifier = Modifier,
    pokemonNotes: Map<Pokemon, String>? = null
) {
    val isCompact = LocalIsCompact.current
    if (isCompact) {
        VerticalPokepaste(pokePaste, pokemonImageService, format, modifier, pokemonNotes)
    } else {
        DesktopPokepaste(pokePaste, pokemonImageService, format, modifier, pokemonNotes)
    }
}

val verticalPokemonSpace = 32.dp

@Composable
fun VerticalPokepaste(
    pokePaste: PokePaste,
    pokemonImageService: PokemonImageService,
    format: Format,
    modifier: Modifier = Modifier,
    pokemonNotes: Map<Pokemon, String>? = null) {
    Column(modifier = modifier) {
        Spacer(Modifier.height(verticalPokemonSpace))
        for (pokemon in pokePaste.pokemons) {
            val notes = pokemonNotes?.get(pokemon)
            PokepastePokemon(pokePaste.isOts, pokemon, pokemonData = null, pokemonImageService, format, Modifier.fillMaxWidth(), notes)
            Spacer(Modifier.height(verticalPokemonSpace))
        }

    }
}

@Composable
private fun DesktopPokepaste(pokePaste: PokePaste, pokemonImageService: PokemonImageService, format: Format, modifier: Modifier, pokemonNotes: Map<Pokemon, String>? = null) {
    Column(modifier = modifier) {
        for (pokemonBlock in pokePaste.pokemons.chunked(3)) {
            DesktopPokemonRow(pokePaste.isOts, pokemonBlock, pokemonImageService, format, pokemonNotes)
        }
    }
}

@Composable
 fun DesktopPokemonRow(
    isOts: Boolean,
    pokemons: List<Pokemon>,
    pokemonImageService: PokemonImageService,
    format: Format,
    pokemonNotes: Map<Pokemon, String>?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = verticalPokemonSpace)
    ) {
        pokemons.forEachIndexed { index, pokemon ->
            if (index > 0) {
                Spacer(Modifier.width(16.dp))
            }
            val notes = pokemonNotes?.get(pokemon)
            PokepastePokemon(isOts, pokemon, pokemonData = null, pokemonImageService, format, Modifier.weight(1f), notes)
        }
    }
}

@Composable
fun PokemonStatsRow(pokemon: Pokemon, pokemonData: PokemonData?, modifier: Modifier = Modifier) {
    Row(modifier) {
        for (stat in listOf(Stat.HP, Stat.ATTACK, Stat.DEFENSE, Stat.SPECIAL_ATTACK, Stat.SPECIAL_DEFENSE, Stat.SPEED)) {
            val modifier = if (LocalIsCompact.current) Modifier.weight(1f) else Modifier.padding(horizontal = 4.dp)
            PokemonStatColumn(pokemon, stat, pokemon.ivs, pokemon.evs, pokemonData?.baseStats, modifier)
        }
    }
}
@Composable
fun PokemonMoves(pokemon: Pokemon, pokemonImageService: PokemonImageService, disableTooltip: Boolean = false, modifier: Modifier = Modifier) {
    Column(modifier) {
        pokemon.moves.forEachIndexed { index, move ->
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val iconModifier = Modifier.size(32.dp)
                pokemonImageService.MoveSpecImages(move, disableTooltip = disableTooltip, iconModifier = iconModifier)
                Spacer(Modifier.width(8.dp))
                val prettyMove = move.pretty
                Text(prettyMove, textAlign = TextAlign.Start, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
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
    baseStats: PokeStats?,
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
        val textColor = when {
            pokemon.nature?.bonusStat == stat -> if (isDarkThemeEnabled()) Color(0xFFE57373) else Color.Red
            pokemon.nature?.malusStat == stat -> if (isDarkThemeEnabled()) Color.Cyan else Color.Blue
            else -> Color.Unspecified
        }
        Text(txt, color = textColor, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        if (baseStats != null) {
            val stats = remember(baseStats) {
                PokeStats.compute(
                    baseStats = baseStats,
                    evs = pokemon.evs,
                    ivs = pokemon.ivs,
                    nature = pokemon.nature ?: Nature.QUIRKY,
                    level = pokemon.level
                )
            }
            StatText(stats[stat], textColor)
        } else {
            StatText(evs[stat], textColor)
            StatText(ivs[stat], textColor)
        }
    }
}

@Composable
private fun StatText(value: Int, textColor: Color) {
    Text(value.toString(), color = textColor, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
}

@Composable
fun PokepastePokemon(
    isOts: Boolean,
    pokemon: Pokemon,
    pokemonData: PokemonData?,
    pokemonImageService: PokemonImageService,
    format: Format,
    modifier: Modifier = Modifier,
    notes: String? = null,
    onClick: (() -> Unit)? = null,
) = PokepastePokemon(
    isOts,
    pokemon,
    pokemonData,
    pokemonImageService,
    format,
    modifier,
    notes,
    onClick
) {
    if (!notes.isNullOrBlank()) {
        Text(notes, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
    }
}

@Composable
fun PokepastePokemon(
    isOts: Boolean,
    pokemon: Pokemon,
    pokemonData: PokemonData?,
    pokemonImageService: PokemonImageService,
    format: Format,
    modifier: Modifier = Modifier,
    notes: String? = null,
    onClick: (() -> Unit)? = null,
    notesComposer: @Composable ColumnScope.() -> Unit
) {
    val megaPokemon = MegaUtils.MEGA_STONE_TO_POKEMON[pokemon.item]
    if (megaPokemon != null && megaPokemon.baseNormalized == pokemon.name.normalized) {
        var megaSelected by mutableStateOf(false)
        // TODO change pokemon name supplied
        PokepastePokemonCard(
            isOts = isOts,
            pokemon = pokemon,
            pokemonData = pokemonData,
            pokemonImageService = pokemonImageService,
            format = format,
            modifier = modifier,
            notes = notes,
            onClick = onClick,
            notesComposer = notesComposer,
            megaSwitch = {

                Switch(
                    megaSelected,
                    onCheckedChange = { megaSelected = it },
                    modifier = Modifier.scale(1.2f).padding(horizontal = 8.dp),
                    thumbContent = {
                        Icon(
                            painter = painterResource(Res.drawable.mega),
                            contentDescription = "Mega"
                        )

                    }
                )
            }
        )

    } else {
        PokepastePokemonCard(
            isOts = isOts,
            pokemon = pokemon,
            pokemonData = pokemonData,
            pokemonImageService = pokemonImageService,
            format = format,
            modifier = modifier,
            notes = notes,
            onClick = onClick,
            notesComposer = notesComposer
        )
    }
}

@Composable
private fun PokepastePokemonCard(
    isOts: Boolean,
    pokemon: Pokemon,
    pokemonData: PokemonData?,
    pokemonImageService: PokemonImageService,
    format: Format,
    modifier: Modifier = Modifier,
    notes: String? = null,
    onClick: (() -> Unit)? = null,
    megaSwitch: (@Composable () -> Unit)? = null,
    notesComposer: @Composable ColumnScope.() -> Unit
) {
    PokemonCard(
        modifier = modifier,
        onClick=onClick,
        pokemonArtwork = { contentWidth, contentHeight ->
            pokemonImageService.PokemonArtwork(
                name = pokemon.name,
                modifier = Modifier.align(Alignment.BottomEnd)
                    .height(if (LocalIsCompact.current) 175.dp else 200.dp)
                    // to avoid artworks like basculegion's to take the whole width and make the moves difficult to read
                    .widthIn(max = remember(contentWidth) { contentWidth * 0.75f })
                    .offset(y = 16.dp)
            )
        }
    ) {
        val disableTooltip = onClick != null
        Column(
            verticalArrangement = Arrangement.Center,
        ) {
            PokepastePokemonHeader(pokemon, pokemonImageService, format=format, megaSwitch=megaSwitch, disableTooltip=disableTooltip)
            Spacer(Modifier.height(16.dp))
            PokepastePokemonItemAndAbility(pokemon, pokemonImageService, disableTooltip=disableTooltip)
            Spacer(Modifier.height(16.dp))

            if (notes != null) {
                notesComposer.invoke(this)
            }
            if (!isOts) {
                PokemonStatsRow(pokemon, pokemonData, Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
            }
            PokemonMoves(pokemon, pokemonImageService, disableTooltip = disableTooltip)
        }
    }
}


@Composable
fun PokepastePokemonHeader(
    pokemon: Pokemon,
    pokemonImageService: PokemonImageService,
    disableTooltip: Boolean = false,
    megaSwitch: @Composable (() -> Unit)? = null,
    format: Format
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(pokemon.name.pretty, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
        if (format.allowedMechanics.contains(Mechanic.MEGA_EVOLUTION)) {
            megaSwitch?.invoke()
        }
        if (format.allowedMechanics.contains(Mechanic.TERASTALLIZATION)) {
            pokemon.teraType?.let {
                pokemonImageService.TeraTypeImage(it, modifier = Modifier.size(if (LocalIsCompact.current) 50.dp else 60.dp), disableTooltip = disableTooltip)
            }
        }
    }
}

@Composable
fun PokepastePokemonItemAndAbility(pokemon: Pokemon, pokemonImageService: PokemonImageService, disableTooltip: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        pokemon.item?.let {
            pokemonImageService.ItemImage(it, modifier = Modifier.size(if (LocalIsCompact.current) 32.dp else 42.dp), disableTooltip = disableTooltip)
        }
        Spacer(Modifier.width(4.dp))
        Text((pokemon.item?.pretty ?: "<no item>") + " | " + (pokemon.ability?.pretty ?: "<no ability>"), style = MaterialTheme.typography.bodyLarge)
    }

}
@Composable
fun PokePasteInput(viewModel: PokepasteEditingViewModel) {
    Column {
        if (platform.type == PlatformType.Web) {
            Text(
                text = "Pokepaste",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pokepaste",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(16.dp))
                OutlinedButton(onClick = { viewModel.showPokepasteUrlDialog() }) {
                    Text("Load from URL")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.pokepaste,
            onValueChange = viewModel::updatePokepaste,
            placeholder = { Text("The pokepaste content (not the URL)") },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            minLines = 8,
            maxLines = 12,
            isError = viewModel.pokepasteError != null,
            supportingText = viewModel.pokepasteError?.let { error ->
                { Text(text = error) }
            }
        )

        if (viewModel.showPokePasteUrlDialog) {
            PokepasteUrlLoadDialog(viewModel)
        }
    }
}

@Composable
private fun PokepasteUrlLoadDialog(viewModel: PokepasteEditingViewModel) {
    val url = viewModel.pokePasteUrlInput
    AlertDialog(
        onDismissRequest = { viewModel.hidePokepasteUrlDialog() },
        title = { Text("Load from URL") },
        text = {
            Column {
                Text(
                    text = "Enter a PokePaste URL:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = viewModel::updatePokepasteUrlInput,
                    label = { Text("URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !viewModel.isLoadingPokepasteUrl,
                    isError = (url.isNotBlank() && !viewModel.isPokepasteUrlValid) || viewModel.pokepasteUrlError != null,
                    supportingText = when {
                        viewModel.pokepasteUrlError != null -> { { Text(viewModel.pokepasteUrlError!!) } }
                        url.isNotBlank() && !viewModel.isPokepasteUrlValid -> { { Text("Please enter a valid URL starting with http:// or https://") } }
                        else -> null
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.loadPokepasteFromUrl() },
                enabled = viewModel.isPokepasteUrlValid && !viewModel.isLoadingPokepasteUrl
            ) {
                Text(if (viewModel.isLoadingPokepasteUrl) "Loading..." else "Load")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { viewModel.hidePokepasteUrlDialog() },
                enabled = !viewModel.isLoadingPokepasteUrl
            ) {
                Text("Cancel")
            }
        }
    )
}
