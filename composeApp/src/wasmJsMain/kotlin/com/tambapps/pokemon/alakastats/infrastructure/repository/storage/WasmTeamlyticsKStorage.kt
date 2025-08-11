package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.storage.storeOf

class WasmTeamlyticsKStorage(
    override val pokepasteParser: PokepasteParser,
) : AbstractTeamlyticsKStorage() {
    override val idsStore: KStore<List<TeamlyticsPreview>> = storeOf(
        key = "repositories/teamlytics-previews.kstore",
        enableCache = false,
    )


    override fun getStore(id: TeamlyticsPreview): KStore<TeamlyticsEntity> = storeOf(
        key = "repositories/${id.id}.kstore",
        enableCache = false,
    )
}