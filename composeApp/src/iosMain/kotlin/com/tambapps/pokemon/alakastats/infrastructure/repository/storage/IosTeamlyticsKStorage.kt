package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.extensions.listStoreOf
import io.github.xxfast.kstore.file.extensions.storeOf
import kotlinx.io.files.Path
import kotlin.uuid.Uuid

class IosTeamlyticsKStorage(
    override val pokepasteParser: PokepasteParser,
) : AbstractTeamlyticsKStorage() {
    override val idsStore: KStore<List<Uuid>> = listStoreOf(
        // TODO probably doesn't work as the paths specified don't exist (see how it was done for android)
        file = Path("repositories/teamlytics-ids.kstore"),
        enableCache = false
    )

    override fun getStore(id: Uuid): KStore<TeamlyticsEntity> = storeOf(
        file = Path("repositories/$id.kstore"),
        enableCache = false,
        version = 0
    )
}