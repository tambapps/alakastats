package com.tambapps.pokemon.alakastats.domain.transformer

import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.domain.model.computeWinRatePercentage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsPreviewEntity
import com.tambapps.pokemon.pokepaste.parser.PokePaste
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import kotlin.time.Clock

class TeamlyticsTransformer(
    private val replayAnalyticsTransformer: ReplayAnalyticsTransformer,
    private val pokepasteParser: PokepasteParser
) {
    
    fun toEntity(domain: Teamlytics): TeamlyticsEntity {
        return TeamlyticsEntity(
            id = domain.id,
            name = domain.name,
            pokePaste = domain.pokePaste.toPokePasteString(),
            replays = domain.replays.map { replayAnalyticsTransformer.toEntity(it) },
            sdNames = domain.sdNames,
            lastUpdatedAt = domain.lastUpdatedAt
        )
    }
    
    fun toDomain(entity: TeamlyticsEntity): Teamlytics {
        return Teamlytics(
            id = entity.id,
            name = entity.name,
            pokePaste = entity.pokePaste.let(pokepasteParser::tryParse) ?: PokePaste(emptyList()),
            replays = entity.replays.map { replayAnalyticsTransformer.toDomain(it) },
            sdNames = entity.sdNames,
            lastUpdatedAt = entity.lastUpdatedAt ?: Clock.System.now()
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