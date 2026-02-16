package com.tambapps.pokemon.alakastats.infrastructure.repository

import arrow.core.Either
import com.tambapps.pokemon.MoveName
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.Stat
import com.tambapps.pokemon.alakastats.domain.error.GetPokemonDataError
import com.tambapps.pokemon.alakastats.domain.model.DamageClass
import com.tambapps.pokemon.alakastats.domain.model.PokemonData
import com.tambapps.pokemon.alakastats.domain.model.PokemonMove
import com.tambapps.pokemon.alakastats.domain.repository.PokemonDataRepository
import com.tambapps.pokemon.pokeapi.client.GqlBatchResult
import com.tambapps.pokemon.pokeapi.client.GqlMove
import com.tambapps.pokemon.pokeapi.client.GqlPokemon
import com.tambapps.pokemon.pokeapi.client.PokeApiGqlClient

class PokeApiPokemonDataRepository(
    private val pokeapiClient: PokeApiGqlClient
): PokemonDataRepository {

    override suspend fun bulkGet(pokemons: List<Pokemon>): Either<GetPokemonDataError, List<PokemonData>> {
        val moves = pokemons.asSequence()
            .flatMap { it.moves }
            .map { it.normalized }
            .distinctBy { it.value }
            .toList()
        return Either.catch {
            pokeapiClient.getPokemonsAndMoves(pokemons.map { it.name.pokeApiNormalized }, moves)
        }.mapLeft { GetPokemonDataError("Couldn't retrieve pokemon data", it) }
            .map { toPokemonData(pokemons, it) }
    }

    private fun toPokemonData(
        pokemons: List<Pokemon>,
        result: GqlBatchResult
    ): List<PokemonData> {
        return pokemons.map { pokemon ->
            val baseStats = result.pokemons.find { it.name == pokemon.name.pokeApiNormalized.value }
                ?.toPokeStats()
            val stats = baseStats?.let {
                PokeStats.compute(
                    baseStats = it,
                    evs = pokemon.evs,
                    ivs = pokemon.ivs,
                    nature = pokemon.nature ?: Nature.QUIRKY,
                    level = pokemon.level
                )
            } ?: PokeStats.default(0)

            PokemonData(
                name = pokemon.name,
                moves = result.moves.filter { pokemon.moves.any { m -> m.normalized.value == it.name } }
                    .map { it.toMove() }
                    .associateBy { it.name.normalized },
                stats
            )
        }
    }
}

private fun GqlMove.toMove() = PokemonMove(
    name = MoveName(name),
    type = PokeType.valueOf(type.name.uppercase()),
    damageClass = DamageClass.valueOf(damageClass.name.uppercase()),
    power = power ?: 0,
    accuracy = accuracy ?: 0
)

private fun GqlPokemon.toPokeStats() = PokeStats(
    hp = findStat(Stat.HP),
    speed = findStat(Stat.SPEED),
    attack = findStat(Stat.ATTACK),
    defense = findStat(Stat.DEFENSE),
    specialAttack = findStat(Stat.SPECIAL_ATTACK),
    specialDefense = findStat(Stat.SPECIAL_DEFENSE),
)

private fun GqlPokemon.findStat(pokeStat: Stat) = stats.find {
    it.stat.name == pokeStat.pokeApiName
}?.baseStat ?: 0

private val PokemonName.pokeApiNormalized: PokemonName get() {
    val n = normalized
    return when {
        n.value.startsWith("ogerpon-") -> PokemonName("ogerpon")
        n.value.endsWith("-f") -> PokemonName(n.value + "emale")
        n.value == "indeedee" -> PokemonName("indeedee-male")
        else -> n
    }
}

private val Stat.pokeApiName: String get() = name.lowercase().replace('_', '-')