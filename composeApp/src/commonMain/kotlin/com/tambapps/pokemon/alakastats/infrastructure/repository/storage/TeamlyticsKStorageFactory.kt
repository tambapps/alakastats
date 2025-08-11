package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import kotlin.uuid.Uuid

expect fun createTeamlyticsKStorage(pokepasteParser: PokepasteParser): KStorage<Uuid, TeamlyticsEntity>