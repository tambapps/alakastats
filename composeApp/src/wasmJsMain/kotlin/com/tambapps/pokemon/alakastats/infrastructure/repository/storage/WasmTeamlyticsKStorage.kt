package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.storage.storeOf
import kotlin.uuid.Uuid

class WasmTeamlyticsKStorage(
    override val pokepasteParser: PokepasteParser,
) : AbstractTeamlyticsKStorage() {
    override val idsStore: KStore<List<Uuid>> = storeOf(
        key = "repositories/teamlytics-ids.kstore",
        enableCache = false,
    )

    override fun getStore(id: Uuid): KStore<TeamlyticsEntity> = storeOf(
        key = "repositories/$id.kstore",
        enableCache = false,
    )
}