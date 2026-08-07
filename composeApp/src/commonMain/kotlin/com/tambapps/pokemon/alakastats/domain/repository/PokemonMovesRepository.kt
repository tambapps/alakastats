package com.tambapps.pokemon.alakastats.domain.repository

import arrow.core.Either
import com.tambapps.pokemon.MoveName
import com.tambapps.pokemon.alakastats.domain.error.GetPokemonDataError
import com.tambapps.pokemon.alakastats.domain.model.PokemonMove

interface PokemonMovesRepository {

    suspend fun getMoves(moves: List<MoveName>): Either<GetPokemonDataError, Map<MoveName, PokemonMove>>

}
