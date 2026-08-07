package com.tambapps.pokemon.alakastats.domain.repository

import arrow.core.Either
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.error.GetPokemonDataError

interface PokemonBaseStatsRepository {

    suspend fun getBaseStats(pokemons: List<PokemonName>): Either<GetPokemonDataError, Map<PokemonName, PokeStats>>

}
