package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import android.content.Context
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual fun createTeamlyticsKStorage(pokepasteParser: PokepasteParser): KStorage<TeamlyticsPreview, TeamlyticsEntity> {
    return object : KoinComponent {
        val context: Context by inject()
    }.let { koinComponent ->
        AndroidTeamlyticsKStorage(pokepasteParser, koinComponent.context)
    }
}