package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.ability
import alakastats.composeapp.generated.resources.move_physical
import androidx.compose.foundation.Image
import com.tambapps.pokemon.PokemonName
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.elevatedCardGradientColors
import com.tambapps.pokemon.alakastats.ui.navigation.AppRoute
import com.tambapps.pokemon.alakastats.ui.navigation.LocalAppNavigator
import com.tambapps.pokemon.alakastats.ui.service.PokemonArtwork
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

// the icon is drawn big, past the card's bottom right corner, faded behind the text
private const val SECTION_ICON_ALPHA = 0.22f
private const val SECTION_ICON_ROTATION = -15f
private val SECTION_ICON_OFFSET = 28.dp

/**
 * The parts of the Pokédex, each reached from its card
 *
 * @property icon the icon of the card, null for a Pokémon's artwork
 * @property artwork the Pokémon whose artwork is the icon of the card
 * @property tinted whether the icon is a monochrome vector, drawn in the card's text color
 */
internal enum class PokedexSection(
    val title: String,
    val description: String,
    val icon: DrawableResource?,
    val route: AppRoute,
    val tinted: Boolean = false,
    val artwork: PokemonName? = null,
) {
    // the Pokémon Alakastats is named after
    POKEMON("Pokémon", "Search a Pokémon, its stats and its forms", null, AppRoute.PokedexPokemon(), artwork = PokemonName("alakazam")),
    ABILITIES("Abilities", "What each ability does, and who has it", Res.drawable.ability, AppRoute.PokedexAbilities, tinted = true),
    MOVES("Moves", "What each move does, and who learns it", Res.drawable.move_physical, AppRoute.PokedexMoves),
}

/**
 * The Pokédex's home screen: a card for each of its parts
 */
@Composable
fun PokedexHomeScreen() {
    val isCompact = LocalIsCompact.current
    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(if (isCompact) Modifier.safeContentPadding() else Modifier.padding(all = 16.dp))
            .verticalScroll(rememberScrollState())
    ) {
        Box(Modifier.fillMaxWidth()) {
            Text(
                "Pokédex",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Spacer(Modifier.height(16.dp))
        if (isCompact) {
            Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (section in PokedexSection.entries) {
                    PokedexSectionCard(section, height = 120.dp, iconSize = 140.dp, Modifier.fillMaxWidth())
                }
            }
        } else {
            // side by side, not wider than what reads well
            Row(
                Modifier.widthIn(max = 1200.dp).align(Alignment.CenterHorizontally).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                for (section in PokedexSection.entries) {
                    PokedexSectionCard(section, height = 180.dp, iconSize = 200.dp, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PokedexSectionCard(section: PokedexSection, height: Dp, iconSize: Dp, modifier: Modifier = Modifier) {
    val navigator = LocalAppNavigator.current
    MyCard(
        onClick = { navigator.push(section.route) },
        modifier = modifier.height(height),
        gradientBackgroundColors = elevatedCardGradientColors,
    ) {
        Box(Modifier.fillMaxSize().clipToBounds()) {
            val iconModifier = Modifier.size(iconSize)
                .align(Alignment.BottomEnd)
                .offset(x = SECTION_ICON_OFFSET, y = SECTION_ICON_OFFSET)
                .alpha(SECTION_ICON_ALPHA)
            if (section.artwork != null) {
                // upright, facing the text
                PokemonArtwork(section.artwork, iconModifier)
            } else if (section.icon != null) {
                Image(
                    painterResource(section.icon),
                    contentDescription = null,
                    colorFilter = if (section.tinted) ColorFilter.tint(MaterialTheme.colorScheme.onSurface) else null,
                    modifier = iconModifier.rotate(SECTION_ICON_ROTATION),
                )
            }
            Column(Modifier.align(Alignment.CenterStart).padding(horizontal = 20.dp)) {
                Text(section.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    section.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    // the icon takes the card's right
                    modifier = Modifier.fillMaxWidth(0.7f),
                )
            }
        }
    }
}
