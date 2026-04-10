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

    override suspend fun bulkGetWithMoves(pokemons: List<Pokemon>): Either<GetPokemonDataError, List<PokemonData>> {
        val moves = pokemons.asSequence()
            .flatMap { it.moves }
            .map { it.normalized }
            .distinctBy { it.value }
            .toList()
        return Either.catch {
            pokeapiClient.getPokemons(pokemons.map { it.name.pokeApiNormalized }, moves)
        }.mapLeft { GetPokemonDataError("Couldn't retrieve pokemon data", it) }
            .map { toPokemonDataWithMoves(pokemons, it) }
    }

    override suspend fun getBaseStats(pokemons: List<PokemonName>): Either<GetPokemonDataError, Map<PokemonName, PokeStats>> {
        return Either.catch {
            pokeapiClient.getPokemons(pokemons.map { it.pokeApiNormalized })
        }.mapLeft { GetPokemonDataError("Couldn't retrieve pokemon data", it) }
            .map { toBaseStatsMap(pokemons, it) }
    }

    private fun toBaseStatsMap(
        pokemons: List<PokemonName>,
        result: GqlBatchResult
    ): Map<PokemonName, PokeStats> = pokemons.associate { pokemonName ->
        val baseStats = result.pokemons.find { it.name == pokemonName.pokeApiNormalized.value }
            ?.toPokeStats() ?: PokeStats.default(0)
        pokemonName.normalized to baseStats
    }

    private fun toPokemonDataWithMoves(
        pokemons: List<Pokemon>,
        result: GqlBatchResult
    ): List<PokemonData> {
        return pokemons.map { pokemon ->
            val baseStats = result.pokemons.find { it.name == pokemon.name.pokeApiNormalized.value }
                ?.toPokeStats()
            PokemonData(
                name = pokemon.name,
                moves = result.moves.filter { pokemon.moves.any { m -> m.normalized.value == it.name } }
                    .map { it.toMove() }
                    .associateBy { it.name.normalized },
                baseStats = baseStats
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

private val MALE_POKEMON_NAMES = listOf("indeedee", "basculegion", "pyroar", "meowstic", "oinkologne")

private val PokemonName.pokeApiNormalized: PokemonName get() {
    val n = normalized
    val name = n.value
    return when {
        name == "landorus" || name == "thundurus" || name == "enamorus" || name == "tornadus" -> PokemonName("$name-incarnate")
        name.startsWith("ogerpon-") -> PokemonName("ogerpon")
        name.endsWith("-f") -> PokemonName(name + "emale")
        name in MALE_POKEMON_NAMES -> PokemonName("$name-male")
        name == "maushold" -> PokemonName("maushold-family-of-four")
        name == "dudunsparce" -> PokemonName("dudunsparce-two-segment")
        name == "tatsugiri" -> PokemonName("tatsugiri-droopy")
        name == "giratina" -> PokemonName("giratina-altered")
        name == "giratina" -> PokemonName("giratina-altered")
        name == "palafin" -> PokemonName("palafin-zero")
        name == "tauros-paldea-combat" -> PokemonName("tauros-paldea-combat-breed")
        name == "tauros-paldea-blaze" -> PokemonName("tauros-paldea-blaze-breed")
        name == "tauros-paldea-aqua" -> PokemonName("tauros-paldea-aqua-breed")
        name == "aegislash" -> PokemonName("aegislash-shield")
        name == "lycanroc" -> PokemonName("lycanroc-midday")
        name == "mimikyu" -> PokemonName("mimikyu-totem-disguised")
        name == "mr.-rime" -> PokemonName("mr-rime")
        name == "morpeko" -> PokemonName("morpeko-full-belly")
        name.startsWith("gourgeist") -> PokemonName("gourgeist-average")
        else -> n
    }
}

private val Stat.pokeApiName: String get() = name.lowercase().replace('_', '-')