package com.tambapps.pokemon.alakastats.domain.transformer

import com.tambapps.pokemon.MoveName
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.DamageClass
import com.tambapps.pokemon.alakastats.domain.model.Format
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

fun Teamlytics.toEntity() = TeamlyticsEntity(
    id = id,
    name = name,
    pokePaste = pokePaste.toPokePasteString(),
    replays = replays.map { it.toEntity() },
    sdNames = sdNames.map { it.value },
    lastUpdatedAt = lastUpdatedAt,
    notes = notes?.toEntity(),
    matchupNotes = matchupNotes.map { it.toEntity() },
    data = data.toEntity(),
    format = format.name
)

fun TeamlyticsEntity.toDomain(pokepasteParser: PokepasteParser): Teamlytics {
    val pokepaste = pokepasteParser.tryParse(pokePaste) ?: PokePaste(emptyList())
    val replays = replays.map { it.toDomain() }
    return Teamlytics(
        id = id,
        name = name,
        pokePaste = pokepaste,
        replays = replays,
        sdNames = sdNames.map(::UserName),
        lastUpdatedAt = lastUpdatedAt ?: Clock.System.now(),
        notes = notes?.toDomain(pokepaste),
        matchupNotes = matchupNotes?.map { it.toDomain(replays, pokepasteParser) } ?: emptyList(),
        data = data?.toDomain() ?: TeamlyticsData(emptyMap()),
        format = format?.let {
            try {
                Format.valueOf(it)
            } catch (e: IllegalArgumentException) {
                Format.NONE
            }
        } ?: Format.NONE
    )
}

fun TeamlyticsEntity.toPreview(pokepasteParser: PokepasteParser, winrate: Int) = TeamlyticsPreviewEntity(
    id = id,
    name = name,
    sdNames = sdNames,
    pokemons = pokepasteParser.tryParse(pokePaste)?.pokemons?.map { it.name.value } ?: emptyList(),
    nbReplays = replays.size,
    winrate = winrate,
    lastUpdatedAt = lastUpdatedAt
)

fun TeamlyticsPreview.toEntity() = TeamlyticsPreviewEntity(
    id = id,
    name = name,
    sdNames = sdNames,
    pokemons = pokemons.map { it.value },
    nbReplays = nbReplays,
    winrate = winrate,
    lastUpdatedAt = lastUpdatedAt
)

fun TeamlyticsPreviewEntity.toDomain() = TeamlyticsPreview(
    id = id,
    name = name,
    sdNames = sdNames,
    pokemons = pokemons.map { PokemonName(it) },
    nbReplays = nbReplays,
    winrate = winrate,
    lastUpdatedAt = lastUpdatedAt ?: Clock.System.now()
)

fun TeamlyticsNotes.toEntity() = TeamlyticsNotesEntity(
    teamNotes = teamNotes,
    pokemonNotes = pokemonNotes.mapKeys { (key, _) -> key.value }
)

fun TeamlyticsNotesEntity.toDomain(pokePaste: PokePaste): TeamlyticsNotes {
    val map = buildMap {
        pokemonNotes.forEach { (key, value) -> put(PokemonName(key), value) }
        // add missing pokemons from pokepaste if any
        for (pokemon in pokePaste.pokemons) {
            if (!containsKey(pokemon.name)) {
                put(pokemon.name, "")
            }
        }
    }
    return TeamlyticsNotes(teamNotes = teamNotes, pokemonNotes = map)
}

fun MatchupNotes.toEntity() = MatchupNotesEntity(
    id = id,
    name = name,
    pokePaste = pokePaste?.toPokePasteString(),
    gamePlans = gamePlans.map { it.toEntity() }
)

fun MatchupNotesEntity.toDomain(replays: List<ReplayAnalytics>, pokepasteParser: PokepasteParser) = MatchupNotes(
    id = id,
    name = name,
    pokePaste = pokePaste?.let(pokepasteParser::tryParse),
    gamePlans = gamePlans.map { it.toDomain(replays) }
)

fun GamePlan.toEntity() = GamePlanEntity(
    description = description,
    composition = composition?.map { it.value },
    exampleReplays = exampleReplays.map { it.reference }
)

fun GamePlanEntity.toDomain(replays: List<ReplayAnalytics>) = GamePlan(
    description = description,
    composition = composition?.map { PokemonName(it) },
    exampleReplays = exampleReplays?.let { refs -> replays.filter { refs.contains(it.reference) } } ?: emptyList()
)

fun TeamlyticsData.toEntity() = TeamlyticsDataEntity(
    pokemonData = pokemonData.entries.associate { (key, value) -> key.value to value.toEntity() }
)

fun PokemonData.toEntity() = PokemonDataEntity(
    name = name.value,
    moves = moves.entries.associate { (key, value) ->
        key.value to PokemonMoveEntity(
            type = value.type.name,
            damageClass = value.damageClass.name,
            power = value.power,
            accuracy = value.accuracy
        )
    },
    stats = listOf(stats.hp, stats.attack, stats.defense, stats.specialAttack, stats.specialDefense, stats.speed)
)

fun TeamlyticsDataEntity.toDomain() = TeamlyticsData(
    pokemonData = pokemonData.entries.associate { (key, value) -> PokemonName(key) to value.toDomain() }
)

fun PokemonDataEntity.toDomain() = PokemonData(
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