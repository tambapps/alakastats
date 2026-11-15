package com.tambapps.pokemon.alakastats.domain.repository

import arrow.core.Either
import com.tambapps.pokemon.alakastats.domain.error.GetPokemonDataError
import com.tambapps.pokemon.alakastats.domain.model.Pokedex

interface PokedexRepository {

    /** The Pokédex of the current Champions format, loaded once */
    suspend fun getPokedex(): Either<GetPokemonDataError, Pokedex>

}
