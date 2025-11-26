package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService


@Composable
fun PokemonFilterChip(
    pokemonName: PokemonName,
    pokemonImageService: PokemonImageService,
    onClick: () -> Unit,
    asLead: Boolean,
    selected: Boolean = asLead,
    modifier: Modifier = Modifier
    ) {
    val height = 70.dp

    FilterChip(
        modifier = modifier.height(height).padding(vertical = 4.dp),
        onClick = onClick,
        leadingIcon = {
            pokemonImageService.PokemonSprite(
                pokemonName,
                disableTooltip = true,
                modifier = Modifier.size(height).padding(bottom = 8.dp)
            )
        },
        label = { Text(
            text = pokemonName.pretty + (if (asLead) "\nas lead" else ""),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall
        ) },
        selected = selected,
        trailingIcon = {
            Text(
                text = "×",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    )
}