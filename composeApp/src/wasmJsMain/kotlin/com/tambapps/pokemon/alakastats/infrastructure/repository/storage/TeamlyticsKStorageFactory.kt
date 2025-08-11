package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser

actual fun createTeamlyticsKStorage(pokepasteParser: PokepasteParser): KStorage<TeamlyticsPreview, TeamlyticsEntity> {
    return WasmTeamlyticsKStorage(pokepasteParser)
}