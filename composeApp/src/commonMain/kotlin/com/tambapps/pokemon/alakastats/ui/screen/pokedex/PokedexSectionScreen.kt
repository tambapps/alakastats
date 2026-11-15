package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton
import com.tambapps.pokemon.alakastats.ui.navigation.LocalAppNavigator
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact

/**
 * A part of the Pokédex not made yet (types, abilities, moves): its title, and a back button to the Pokédex's home
 */
@Composable
internal fun PokedexSectionScreen(section: PokedexSection) {
    val navigator = LocalAppNavigator.current
    val isCompact = LocalIsCompact.current
    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(if (isCompact) Modifier.safeContentPadding() else Modifier.padding(all = 16.dp))
    ) {
        Box(Modifier.fillMaxWidth()) {
            BackIconButton(navigator, Modifier.align(Alignment.CenterStart))
            Text(
                section.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(
            "Coming soon",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}
