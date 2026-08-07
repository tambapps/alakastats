package com.tambapps.pokemon.alakastats.infrastructure.repository

import alakastats.composeapp.generated.resources.Res
import arrow.core.Either
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.error.GetPokemonDataError
import com.tambapps.pokemon.alakastats.domain.repository.PokemonBaseStatsRepository
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PokemonBaseStatsEntity(
    val baseStats: BaseStatsEntity
)

@Serializable
data class BaseStatsEntity(
    val hp: Int = 0,
    val attack: Int = 0,
    val defense: Int = 0,
    @SerialName("special-attack") val specialAttack: Int = 0,
    @SerialName("special-defense") val specialDefense: Int = 0,
    val speed: Int = 0
)

class LocalPokemonBaseStatsRepository(
    private val json: Json
): PokemonBaseStatsRepository {

    private var cache: Map<String, PokemonBaseStatsEntity>? = null

    override suspend fun getBaseStats(pokemons: List<PokemonName>): Either<GetPokemonDataError, Map<PokemonName, PokeStats>> {
        return Either.catch {
            val data = loadData()
            pokemons.associate { pokemonName ->
                val baseStats = data[pokemonName.normalized.value]?.baseStats?.toPokeStats() ?: PokeStats.default(0)
                pokemonName.normalized to baseStats
            }
        }.mapLeft { GetPokemonDataError("Couldn't load pokemon data", it) }
    }

    private suspend fun loadData(): Map<String, PokemonBaseStatsEntity> {
        return cache ?: Res.readBytes("files/pokemon-stats.json").decodeToString()
            .let { json.decodeFromString<Map<String, PokemonBaseStatsEntity>>(it) }
            .also { cache = it }
    }
}

private fun BaseStatsEntity.toPokeStats() = PokeStats(
    hp = hp,
    speed = speed,
    attack = attack,
    defense = defense,
    specialAttack = specialAttack,
    specialDefense = specialDefense,
)
