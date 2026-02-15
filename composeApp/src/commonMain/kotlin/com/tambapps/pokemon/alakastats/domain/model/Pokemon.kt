package com.tambapps.pokemon.alakastats.domain.model

import com.tambapps.pokemon.MoveName
import com.tambapps.pokemon.PokeType

enum class DamageClass {
    PHYSICAL,
    SPECIAL,
    STATUS
}

data class PokemonMove(
    val name: MoveName,
    val type: PokeType,
    val damageClass: DamageClass,
    val power: Int,
    val accuracy: Int
)