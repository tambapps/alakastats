package com.tambapps.pokemon.alakastats.domain.model

import com.tambapps.pokemon.MoveName
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.PokemonName


data class PokemonData(
    val name: PokemonName,
    val moves: Map<MoveName, PokemonMove>,
    val stats: PokeStats
)

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