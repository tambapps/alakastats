package com.tambapps.pokemon.alakastats.infrastructure.repository

import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.domain.model.computeWinRate
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.alakastats.domain.transformer.ReplayAnalyticsTransformer
import com.tambapps.pokemon.alakastats.domain.transformer.TeamlyticsTransformer
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.KStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import kotlin.uuid.Uuid

class KStoreTeamlyticsRepository(
    private val storage: KStorage<Uuid, TeamlyticsEntity>,
    private val pokepasteParser: PokepasteParser,
    private val teamlyticsTransformer: TeamlyticsTransformer,
    private val replayTransformer: ReplayAnalyticsTransformer
): TeamlyticsRepository {

    override suspend fun listPreviews() = storage.listEntities().map { team ->
        TeamlyticsPreview(
            id = team.id,
            name = team.name,
            sdNames = team.sdNames,
            pokemons = pokepasteParser.tryParse(team.pokePaste)?.pokemons?.map { it.name } ?: emptyList(),
            nbReplays = team.replays.size,
            winrate = computeWinRate(team.sdNames, team.replays.map(replayTransformer::toDomain))
        )

    }
    override suspend fun list() = storage.listEntities().map(teamlyticsTransformer::toDomain)

    override suspend fun save(teamlytics: Teamlytics) = storage.save(teamlyticsTransformer.toEntity(teamlytics))
        .let(teamlyticsTransformer::toDomain)

    override suspend fun delete(teamlytics: Teamlytics) = storage.delete(teamlyticsTransformer.toEntity(teamlytics))
}
