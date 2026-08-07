package com.tambapps.pokemon.alakastats.infrastructure.repository

import arrow.core.Either
import com.tambapps.pokemon.MoveName
import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.alakastats.domain.error.GetPokemonDataError
import com.tambapps.pokemon.alakastats.domain.model.DamageClass
import com.tambapps.pokemon.alakastats.domain.model.PokemonMove
import com.tambapps.pokemon.alakastats.domain.repository.PokemonMovesRepository
import com.tambapps.pokemon.pokeapi.client.GqlMove
import com.tambapps.pokemon.pokeapi.client.PokeApiGqlClient

class PokeApiPokemonMovesRepository(
    private val pokeapiClient: PokeApiGqlClient
): PokemonMovesRepository {

    override suspend fun getMoves(moves: List<MoveName>): Either<GetPokemonDataError, Map<MoveName, PokemonMove>> {
        return Either.catch {
            pokeapiClient.getPokemons(emptyList(), moves)
        }.mapLeft { GetPokemonDataError("Couldn't retrieve move data", it) }
            .map { result -> result.moves.map { it.toMove() }.associateBy { it.name.normalized } }
    }
}

private fun GqlMove.toMove() = PokemonMove(
    name = MoveName(name),
    type = PokeType.valueOf(type.name.uppercase()),
    damageClass = DamageClass.valueOf(damageClass.name.uppercase()),
    power = power ?: 0,
    accuracy = accuracy ?: 0
)
