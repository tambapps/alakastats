package com.tambapps.pokemon.alakastats.domain.transformer

import com.tambapps.pokemon.MoveName
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.DamageClass
import com.tambapps.pokemon.alakastats.domain.model.GamePlan
import com.tambapps.pokemon.alakastats.domain.model.MatchupNotes
import com.tambapps.pokemon.alakastats.domain.model.PokemonData
import com.tambapps.pokemon.alakastats.domain.model.PokemonMove
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsData
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsNotes
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.domain.model.UserName
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.GamePlanEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.MatchupNotesEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.PokemonDataEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.PokemonMoveEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsDataEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsNotesEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsPreviewEntity
import com.tambapps.pokemon.pokepaste.parser.PokePaste
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import kotlin.time.Clock

class TeamlyticsTransformer(
    private val replayAnalyticsTransformer: ReplayAnalyticsTransformer,
    private val notesTransformer: TeamlyticsNotesTransformer,
    private val matchupNotesTransformer: MatchupNotesTransformer,
    private val pokepasteParser: PokepasteParser
) {
    
    fun toEntity(domain: Teamlytics): TeamlyticsEntity {
        return TeamlyticsEntity(
            id = domain.id,
            name = domain.name,
            pokePaste = domain.pokePaste.toPokePasteString(),
            replays = domain.replays.map { replayAnalyticsTransformer.toEntity(it) },
            sdNames = domain.sdNames.map { it.value },
            lastUpdatedAt = domain.lastUpdatedAt,
            notes = domain.notes?.let(notesTransformer::toEntity),
            matchupNotes = domain.matchupNotes.map(matchupNotesTransformer::toEntity),
            data = domain.data.toEntity()
        )
    }
    
    fun toDomain(entity: TeamlyticsEntity): Teamlytics {
        val pokepaste = entity.pokePaste.let(pokepasteParser::tryParse) ?: PokePaste(emptyList())
        val replays = entity.replays.map { replayAnalyticsTransformer.toDomain(it) }
        return Teamlytics(
            id = entity.id,
            name = entity.name,
            pokePaste = pokepaste,
            replays = replays,
            sdNames = entity.sdNames.map(::UserName),
            lastUpdatedAt = entity.lastUpdatedAt ?: Clock.System.now(),
            notes = entity.notes?.let { notesTransformer.toDomain(pokepaste, it) },
            matchupNotes = entity.matchupNotes?.map { matchupNotesTransformer.toDomain(replays, it) } ?: emptyList(),
            data = entity.data?.toDomain() ?: TeamlyticsData(emptyMap())
        )
    }

    fun toPreview(team: TeamlyticsEntity, winrate: Int) = TeamlyticsPreviewEntity(
        id = team.id,
        name = team.name,
        sdNames = team.sdNames,
        pokemons = pokepasteParser.tryParse(team.pokePaste)?.pokemons?.map { it.name.value } ?: emptyList(),
        nbReplays = team.replays.size,
        winrate = winrate,
        lastUpdatedAt = team.lastUpdatedAt
    )

}

class TeamlyticsPreviewTransformer {

    fun toEntity(domain: TeamlyticsPreview): TeamlyticsPreviewEntity {
        return TeamlyticsPreviewEntity(
            id = domain.id,
            name = domain.name,
            sdNames = domain.sdNames,
            pokemons = domain.pokemons.map { it.value },
            nbReplays = domain.nbReplays,
            winrate = domain.winrate,
            lastUpdatedAt = domain.lastUpdatedAt
        )
    }

    fun toDomain(entity: TeamlyticsPreviewEntity): TeamlyticsPreview {
        return TeamlyticsPreview(
            id = entity.id,
            name = entity.name,
            sdNames = entity.sdNames,
            pokemons = entity.pokemons.map { PokemonName(it) },
            nbReplays = entity.nbReplays,
            winrate = entity.winrate,
            lastUpdatedAt = entity.lastUpdatedAt ?: Clock.System.now()
        )
    }
}


class TeamlyticsNotesTransformer {

    fun toDomain(pokePaste: PokePaste, entity: TeamlyticsNotesEntity): TeamlyticsNotes {
        val map = buildMap {
            entity.pokemonNotes.forEach { (key, value) -> put(PokemonName(key), value) }
            // add missing pokemons from pokepaste if any
            for (pokemon in pokePaste.pokemons) {
                if (!containsKey(pokemon.name)) {
                    put(pokemon.name, "")
                }
            }
        }
        return TeamlyticsNotes(
            teamNotes = entity.teamNotes,
            pokemonNotes = map
        )
    }

    fun toEntity(domain: TeamlyticsNotes): TeamlyticsNotesEntity {
        return TeamlyticsNotesEntity(
            teamNotes = domain.teamNotes,
            pokemonNotes = domain.pokemonNotes.mapKeys { (key, _) -> key.value }
        )
    }
}

class MatchupNotesTransformer(
    private val gamePlanTransformer: GamePlanTransformer,
    private val pokepasteParser: PokepasteParser
) {

    fun toEntity(domain: MatchupNotes) = MatchupNotesEntity(
        id = domain.id,
        name = domain.name,
        pokePaste = domain.pokePaste?.toPokePasteString(),
        gamePlans = domain.gamePlans.map(gamePlanTransformer::toEntity)
    )

    fun toDomain(replays: List<ReplayAnalytics>, entity: MatchupNotesEntity) = MatchupNotes(
        id = entity.id,
        name = entity.name,
        pokePaste = entity.pokePaste?.let(pokepasteParser::tryParse),
        gamePlans = entity.gamePlans.map { gamePlanTransformer.toDomain(replays, it) }
    )
}

class GamePlanTransformer {

    fun toEntity(domain: GamePlan) = GamePlanEntity(
        description = domain.description,
        composition = domain.composition?.map { it.value },
        exampleReplays = domain.exampleReplays.map { it.reference }
    )

    fun toDomain(replays: List<ReplayAnalytics>, entity: GamePlanEntity) = GamePlan(
        description = entity.description,
        composition = entity.composition?.map { PokemonName(it) },
        exampleReplays = entity.exampleReplays?.let { exampleReplayRefs ->
            replays.filter { exampleReplayRefs.contains(it.reference) }
        } ?: emptyList()
    )
}

private fun TeamlyticsData.toEntity() = TeamlyticsDataEntity(
    pokemonData = pokemonData.entries.associate { (key, value) -> key.value to value.toEntity() }
)

private fun PokemonData.toEntity() = PokemonDataEntity(
    name = name.value,
    moves = moves.entries.associate { (key, value) -> key.value to PokemonMoveEntity(
        type = value.type.name,
        damageClass = value.damageClass.name,
        power = value.power,
        accuracy = value.accuracy
    )
    },
    stats = listOf(stats.hp, stats.attack, stats.defense, stats.specialAttack, stats.specialDefense, stats.speed)
)

private fun TeamlyticsDataEntity.toDomain() = TeamlyticsData(
    pokemonData = pokemonData.entries.associate { (key, value) -> PokemonName(key) to value.toDomain() }
)

private fun PokemonDataEntity.toDomain() = PokemonData(
    name = PokemonName(name),
    moves = moves.entries.associate { (moveName, moveEntity) ->
        val move = PokemonMove(
            name = MoveName(moveName),
            type = PokeType.valueOf(moveEntity.type),
            damageClass = DamageClass.valueOf(moveEntity.damageClass),
            power = moveEntity.power,
            accuracy = moveEntity.accuracy
        )
        move.name to move
    },
    stats = PokeStats(
        hp = stats.getOrElse(0) { 0 },
        attack = stats.getOrElse(1) { 0 },
        defense = stats.getOrElse(2) { 0 },
        specialAttack = stats.getOrElse(3) { 0 },
        specialDefense = stats.getOrElse(4) { 0 },
        speed = stats.getOrElse(5) { 0 }
    )
)