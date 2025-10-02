package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tambapps.pokemon.alakastats.domain.model.Player
import com.tambapps.pokemon.alakastats.ui.service.IPokemonImageService

@Composable
fun PokemonTeamPreview(imageService: IPokemonImageService, player: Player) {
    PokemonTeamPreview(imageService, player.teamPreview.pokemons.map { it.name })
}

@Composable
fun PokemonTeamPreview(imageService: IPokemonImageService, pokemons: List<String>) {
    Row {
        for (pokemon in pokemons) {
            imageService.PokemonSprite(
                name = pokemon,
                modifier = Modifier.weight(1f),
                // disabling tooltip to allow handling card click listener
                disableTooltip = true
            )
        }
    }
}