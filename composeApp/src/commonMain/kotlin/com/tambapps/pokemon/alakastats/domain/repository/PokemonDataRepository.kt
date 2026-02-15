package com.tambapps.pokemon.alakastats.domain.repository

import arrow.core.Either
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.alakastats.domain.error.GetPokemonDataError
import com.tambapps.pokemon.alakastats.domain.model.PokemonData

interface PokemonDataRepository {

    suspend fun bulkGet(pokemons: List<Pokemon>): Either<GetPokemonDataError, List<PokemonData>>

}