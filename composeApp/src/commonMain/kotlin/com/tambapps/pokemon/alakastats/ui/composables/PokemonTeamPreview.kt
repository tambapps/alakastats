package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.Player
import com.tambapps.pokemon.alakastats.ui.service.FacingDirection
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService

@Composable
fun PokemonTeamPreview(
    imageService: PokemonImageService,
    player: Player,
    childModifier: Modifier = Modifier,
    fillWidth: Boolean = false,
    facingDirection: FacingDirection = FacingDirection.LEFT,
    ) {
    PokemonTeamPreview(imageService, player.teamPreview.pokemons.map { it.name }, childModifier, fillWidth, facingDirection)
}

@Composable
fun PokemonTeamPreview(
    imageService: PokemonImageService,
    pokemons: List<PokemonName>,
    childModifier: Modifier = Modifier,
    fillWidth: Boolean = false,
    facingDirection: FacingDirection = FacingDirection.LEFT) {
    Row {
        for (pokemon in pokemons) {
            val modifier = if (fillWidth) childModifier.weight(1f) else childModifier
            imageService.PokemonSprite(
                name = pokemon,
                modifier = modifier,
                facingDirection = facingDirection,
                // disabling tooltip to allow handling card click listener
                disableTooltip = true
            )
        }
    }
}