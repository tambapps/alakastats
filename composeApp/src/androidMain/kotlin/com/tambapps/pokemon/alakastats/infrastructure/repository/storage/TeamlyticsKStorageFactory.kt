package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import android.content.Context
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import kotlin.uuid.Uuid
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual fun createTeamlyticsKStorage(pokepasteParser: PokepasteParser): KStorage<Uuid, TeamlyticsEntity> {
    return object : KoinComponent {
        val context: Context by inject()
    }.let { koinComponent ->
        AndroidTeamlyticsKStorage(pokepasteParser, koinComponent.context)
    }
}