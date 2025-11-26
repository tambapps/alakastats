package com.tambapps.pokemon.alakastats.domain.transformer

import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.GamePlan
import com.tambapps.pokemon.alakastats.domain.model.MatchupNotes
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsNotes
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.domain.model.computeWinRatePercentage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.GamePlanEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.MatchupNotesEntity
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
            sdNames = domain.sdNames,
            lastUpdatedAt = domain.lastUpdatedAt,
            notes = domain.notes?.let(notesTransformer::toEntity),
            matchupNotes = domain.matchupNotes.map(matchupNotesTransformer::toEntity)
        )
    }
    
    fun toDomain(entity: TeamlyticsEntity): Teamlytics {
        val pokepaste = entity.pokePaste.let(pokepasteParser::tryParse) ?: PokePaste(emptyList())
        return Teamlytics(
            id = entity.id,
            name = entity.name,
            pokePaste = pokepaste,
            replays = entity.replays.map { replayAnalyticsTransformer.toDomain(it) },
            sdNames = entity.sdNames,
            lastUpdatedAt = entity.lastUpdatedAt ?: Clock.System.now(),
            notes = entity.notes?.let { notesTransformer.toDomain(pokepaste, it) },
            matchupNotes = entity.matchupNotes?.map(matchupNotesTransformer::toDomain) ?: emptyList()
        )
    }

    fun toPreview(team: TeamlyticsEntity) = TeamlyticsPreviewEntity(
        id = team.id,
        name = team.name,
        sdNames = team.sdNames,
        pokemons = pokepasteParser.tryParse(team.pokePaste)?.pokemons?.map { it.name.value } ?: emptyList(),
        nbReplays = team.replays.size,
        winrate = computeWinRatePercentage(team.sdNames, team.replays.map(replayAnalyticsTransformer::toDomain)),
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
        name = domain.name,
        pokePaste = domain.pokePaste?.toPokePasteString(),
        gamePlans = domain.gamePlans.map(gamePlanTransformer::toEntity)
    )

    fun toDomain(entity: MatchupNotesEntity) = MatchupNotes(
        name = entity.name,
        pokePaste = entity.pokePaste?.let(pokepasteParser::tryParse),
        gamePlans = entity.gamePlans.map(gamePlanTransformer::toDomain)
    )
}

class GamePlanTransformer {

    fun toEntity(domain: GamePlan) = GamePlanEntity(
        description = domain.description,
        composition = domain.composition?.map { it.value }
    )

    fun toDomain(entity: GamePlanEntity) = GamePlan(
        description = entity.description,
        composition = entity.composition?.map { PokemonName(it) }
    )
}