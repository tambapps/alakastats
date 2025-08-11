package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import android.content.Context
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.extensions.listStoreOf
import io.github.xxfast.kstore.file.extensions.storeOf
import kotlinx.io.files.Path
import java.io.File
import kotlin.uuid.Uuid

class AndroidTeamlyticsKStorage(
    override val pokepasteParser: PokepasteParser,
    private val context: Context
) : AbstractTeamlyticsKStorage() {
    
    private val repositoriesDir = File(context.filesDir, "repositories").apply {
        mkdirs() // Ensure the directory exists
    }
    
    override val idsStore: KStore<List<Uuid>> = listStoreOf(
        file = Path(repositoriesDir.absolutePath, "teamlytics-ids.kstore"),
        enableCache = false
    )

    override fun getStore(id: Uuid): KStore<TeamlyticsEntity> = storeOf(
        file = Path(repositoriesDir.absolutePath, "$id.kstore"),
        enableCache = false,
        version = 0
    )
}