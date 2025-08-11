package com.tambapps.pokemon.alakastats.domain.transformer

import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.domain.model.computeWinRate
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.ReplayAnalyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsPreviewEntity
import com.tambapps.pokemon.pokepaste.parser.PokePaste
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser

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
            sdNames = domain.sdNames
        )
    }
    
    fun toDomain(entity: TeamlyticsEntity): Teamlytics {
        return Teamlytics(
            id = entity.id,
            name = entity.name,
            pokePaste = entity.pokePaste.let(pokepasteParser::tryParse) ?: PokePaste(emptyList()),
            replays = entity.replays.map { replayAnalyticsTransformer.toDomain(it) },
            sdNames = entity.sdNames
        )
    }

    fun toPreview(team: TeamlyticsEntity) = TeamlyticsPreviewEntity(
        id = team.id,
        name = team.name,
        sdNames = team.sdNames,
        pokemons = pokepasteParser.tryParse(team.pokePaste)?.pokemons?.map { it.name } ?: emptyList(),
        nbReplays = team.replays.size,
        winrate = computeWinRate(team.sdNames, team.replays.map(replayAnalyticsTransformer::toDomain))
    )

}

class TeamlyticsPreviewTransformer {

    fun toEntity(domain: TeamlyticsPreview): TeamlyticsPreviewEntity {
        return TeamlyticsPreviewEntity(
            id = domain.id,
            name = domain.name,
            sdNames = domain.sdNames,
            pokemons = domain.pokemons,
            nbReplays = domain.nbReplays,
            winrate = domain.winrate,
        )
    }

    fun toDomain(entity: TeamlyticsPreviewEntity): TeamlyticsPreview {
        return TeamlyticsPreview(
            id = entity.id,
            name = entity.name,
            sdNames = entity.sdNames,
            pokemons = entity.pokemons,
            nbReplays = entity.nbReplays,
            winrate = entity.winrate,
        )
    }
}


class ReplayAnalyticsTransformer(
    private val sdReplayTransformer: SdReplayTransformer
) {
    
    fun toEntity(domain: ReplayAnalytics): ReplayAnalyticsEntity {
        return ReplayAnalyticsEntity(
            replay = sdReplayTransformer.toEntity(domain.replay)
        )
    }
    
    fun toDomain(entity: ReplayAnalyticsEntity): ReplayAnalytics {
        return ReplayAnalytics(
            replay = sdReplayTransformer.toDomain(entity.replay)
        )
    }
}