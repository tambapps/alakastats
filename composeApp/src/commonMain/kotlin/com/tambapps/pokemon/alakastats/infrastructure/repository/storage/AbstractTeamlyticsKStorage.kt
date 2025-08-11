package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import kotlin.uuid.Uuid

abstract class AbstractTeamlyticsKStorage: AbstractKStorage<Uuid, TeamlyticsEntity>() {

    abstract val pokepasteParser: PokepasteParser

    override fun getId(entity: TeamlyticsEntity): Uuid = entity.id

}