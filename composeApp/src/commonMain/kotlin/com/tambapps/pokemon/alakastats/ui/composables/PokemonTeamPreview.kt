package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.Player
import com.tambapps.pokemon.alakastats.ui.service.FacingDirection
import com.tambapps.pokemon.alakastats.ui.service.PokemonSprite
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact

@Composable
fun PokemonTeamPreview(
    player: Player,
    fillWidth: Boolean = false,
    modifier: Modifier = Modifier,
    facingDirection: FacingDirection = FacingDirection.LEFT,
    ) {
    PokemonTeamPreview(player.teamPreview.pokemons.map { it.name }, modifier, fillWidth, facingDirection)
}

@Composable
fun PokemonTeamPreview(
    pokemons: List<PokemonName>,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = false,
    facingDirection: FacingDirection = FacingDirection.LEFT) {
    Row(
        modifier = modifier.heightIn(max = if (LocalIsCompact.current) 60.dp else 80.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (pokemon in pokemons) {
            PokemonSprite(
                name = pokemon,
                modifier = if (fillWidth) Modifier.weight(1f) else Modifier,
                facingDirection = facingDirection,
            )
        }
    }
}