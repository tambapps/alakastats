package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser

// TODO separate model between storage and business?
abstract class AbstractTeamlyticsKStorage: AbstractKStorage<TeamlyticsPreview, TeamlyticsEntity>() {

    abstract val pokepasteParser: PokepasteParser

    override fun getId(entity: TeamlyticsEntity) = TeamlyticsPreview(
        id = entity.id,
        name = entity.name,
        pokemons = entity.pokePaste?.let(pokepasteParser::tryParse)
            ?.let { pokepaste -> pokepaste.pokemons.map { it.name } } ?: emptyList(),
        nbReplays = entity.replays.size,
        winrate = 50 // TODO
    )

}