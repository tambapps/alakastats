package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.navigation.LocalAppNavigator
import org.koin.compose.viewmodel.koinViewModel
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.Stat
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton
import com.tambapps.pokemon.alakastats.ui.composables.SuggestionTextField
import com.tambapps.pokemon.alakastats.ui.service.PokemonArtwork
import com.tambapps.pokemon.alakastats.ui.service.PokemonSprite
import com.tambapps.pokemon.alakastats.ui.service.availablePokemonNames
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact

private const val MAX_STAT_POINTS = 32
// the bars are full from this base stat: the few stats above it (e.g. Blissey's HP) would flatten all the others
private const val FULL_BAR_BASE_STAT = 180
private const val STAT_BAR_ANIMATION_MILLIS = 350
private const val ARTWORK_CROSSFADE_MILLIS = 250

private val STAT_NAME_WIDTH = 36.dp
private val STAT_VALUE_WIDTH = 36.dp
private val FORM_SIZE = 64.dp

private val DISPLAYED_STATS = listOf(
    Stat.HP to "HP",
    Stat.ATTACK to "Atk",
    Stat.DEFENSE to "Def",
    Stat.SPECIAL_ATTACK to "SpA",
    Stat.SPECIAL_DEFENSE to "SpD",
    Stat.SPEED to "Spe",
)

@Composable
fun PokedexScreen() {
        val viewModel = koinViewModel<PokedexViewModel>()
        val navigator = LocalAppNavigator.current
        val isCompact = LocalIsCompact.current
        val focusManager = LocalFocusManager.current
        val availableNames = availablePokemonNames()
        // the names load in one go. The megas are reached from their base form, with the form selector
        val searchablePokemons = remember(availableNames.size) { availableNames.filterNot { it.isMega } }
        Column(
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .then(if (isCompact) Modifier.safeContentPadding() else Modifier.padding(all = 16.dp))
        ) {
            Box(Modifier.fillMaxWidth()) {
                BackIconButton(Modifier.align(Alignment.CenterStart)) { navigator.pop() }
                Text(
                    "Pokédex",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(Modifier.height(8.dp))
            SuggestionTextField(
                value = viewModel.query,
                onValueChange = { viewModel.query = it },
                suggestions = searchablePokemons,
                suggestionText = { it.pretty },
                label = "Search a Pokémon",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                suggestionLeadingContent = { PokemonSprite(it, Modifier.size(32.dp)) },
                onSuggestionPicked = {
                    // on a phone, the keyboard would hide the sheet
                    focusManager.clearFocus()
                    viewModel.selectPokemon(it)
                },
            )
            Spacer(Modifier.height(16.dp))
            val pokemon = viewModel.selectedForm
            if (pokemon == null) {
                Text(
                    "Search a Pokémon to see its stats",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            } else {
                PokemonSheet(viewModel, pokemon, isCompact)
            }
        }
}

@Composable
private fun PokemonSheet(viewModel: PokedexViewModel, pokemon: PokemonName, isCompact: Boolean) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(pokemon.pretty, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        FormSelector(viewModel)
        Spacer(Modifier.height(8.dp))
        if (isCompact) {
            CrossfadingArtwork(pokemon, Modifier.size(200.dp))
            Spacer(Modifier.height(16.dp))
            PokemonStats(viewModel, Modifier.fillMaxWidth())
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CrossfadingArtwork(pokemon, Modifier.size(320.dp))
                }
                PokemonStats(viewModel, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CrossfadingArtwork(pokemon: PokemonName, modifier: Modifier = Modifier) {
    Crossfade(pokemon, modifier, animationSpec = tween(ARTWORK_CROSSFADE_MILLIS)) {
        PokemonArtwork(it, Modifier.fillMaxSize())
    }
}

/**
 * The forms of the pokemon, as clickable sprites, for a pokemon having several of them
 */
@Composable
private fun FormSelector(viewModel: PokedexViewModel) {
    if (viewModel.forms.size < 2) return
    Row(
        Modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (form in viewModel.forms) {
            val background = if (form == viewModel.selectedForm) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            Box(
                Modifier.size(FORM_SIZE)
                    .clip(RoundedCornerShape(12.dp))
                    .background(background)
                    .clickable { viewModel.selectForm(form) }
                    .padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                PokemonSprite(form, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun PokemonStats(viewModel: PokedexViewModel, modifier: Modifier = Modifier) {
    val baseStats = viewModel.baseStats
    Box(modifier, contentAlignment = Alignment.Center) {
        when {
            viewModel.isLoading -> CircularProgressIndicator()
            baseStats == null -> Text(
                "The stats of this Pokémon aren't available",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> StatsTable(baseStats)
        }
    }
}

/**
 * The base stats and, for each of them, its level 50 values: max (all the stat points and a nature boosting it),
 * neutral (all the stat points, a neutral nature), without stat points (a neutral nature) and min (no stat points, a
 * nature lowering it)
 */
@Composable
private fun StatsTable(baseStats: PokeStats) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(STAT_NAME_WIDTH))
            StatHeader("Base")
            Spacer(Modifier.weight(1f))
            StatHeader("Max")
            StatHeader("Neu")
            StatHeader("0 SP")
            StatHeader("Min")
        }
        for ((stat, name) in DISPLAYED_STATS) {
            StatRow(stat, name, baseStats)
        }
    }
}

@Composable
private fun StatRow(stat: Stat, name: String, baseStats: PokeStats) {
    val values = remember(baseStats, stat) {
        val boosting = Nature.entries.firstOrNull { it.bonusStat == stat } ?: Nature.QUIRKY
        val lowering = Nature.entries.firstOrNull { it.malusStat == stat } ?: Nature.QUIRKY
        listOf(
            computeStats(baseStats, MAX_STAT_POINTS, boosting),
            computeStats(baseStats, MAX_STAT_POINTS, Nature.QUIRKY),
            computeStats(baseStats, statPoints = 0, Nature.QUIRKY),
            computeStats(baseStats, statPoints = 0, lowering),
        ).map { it[stat] }
    }
    // the speed is what's looked up the most: it stands out
    val isHighlighted = stat == Stat.SPEED
    val valueWeight = if (isHighlighted) FontWeight.Bold else null
    val background = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else LocalContentColor.current
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Row(
            Modifier.background(background, RoundedCornerShape(8.dp)).padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(STAT_NAME_WIDTH),
            )
            StatValue(baseStats[stat], fontWeight = FontWeight.Bold)
            StatBar(baseStats[stat], Modifier.weight(1f).padding(horizontal = 4.dp))
            for (value in values) {
                StatValue(value, valueWeight)
            }
        }
    }
}

private fun computeStats(baseStats: PokeStats, statPoints: Int, nature: Nature) =
    PokeStats.compute(baseStats = baseStats, statPoints = PokeStats.default(statPoints), nature = nature)

@Composable
private fun StatHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier.width(STAT_VALUE_WIDTH),
    )
}

@Composable
private fun StatValue(value: Int, fontWeight: FontWeight? = null) {
    Text(
        value.toString(),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = fontWeight,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(STAT_VALUE_WIDTH),
    )
}

@Composable
private fun StatBar(baseStat: Int, modifier: Modifier = Modifier) {
    // grows or shrinks when changing of form
    val fraction by animateFloatAsState(
        targetValue = (baseStat.toFloat() / FULL_BAR_BASE_STAT).coerceIn(0f, 1f),
        animationSpec = tween(STAT_BAR_ANIMATION_MILLIS),
    )
    val color by animateColorAsState(statColor(baseStat), animationSpec = tween(STAT_BAR_ANIMATION_MILLIS))
    Box(modifier.height(12.dp)) {
        Box(
            Modifier.fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(color, RoundedCornerShape(4.dp))
        )
    }
}

private fun statColor(baseStat: Int) = when {
    baseStat < 60 -> Color(0xFFF44336)
    baseStat < 90 -> Color(0xFFFF9800)
    baseStat < 110 -> Color(0xFFFFC107)
    baseStat < 130 -> Color(0xFF8BC34A)
    else -> Color(0xFF00BFA5)
}
