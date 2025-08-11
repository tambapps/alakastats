package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import android.content.Context
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.extensions.listStoreOf
import io.github.xxfast.kstore.file.extensions.storeOf
import kotlinx.io.files.Path

class AndroidTeamlyticsKStorage(
    override val pokepasteParser: PokepasteParser,
    private val context: Context
) : AbstractTeamlyticsKStorage() {
    override val idsStore: KStore<List<TeamlyticsPreview>> = listStoreOf(
        file = Path(context.filesDir.absolutePath, "repositories/teamlytics-preview.kstore"),
        enableCache = false
    )

    override fun getStore(id: TeamlyticsPreview): KStore<TeamlyticsEntity> = storeOf(
        file = Path(context.filesDir.absolutePath, "repositories/${id.id}.kstore"),
        enableCache = false,
        version = 0
    )
}