package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsPreviewEntity
import com.tambapps.pokemon.alakastats.util.Identifiable
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write
import io.github.xxfast.kstore.file.extensions.listStoreOf
import io.github.xxfast.kstore.file.extensions.storeOf
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid
import org.koin.core.component.KoinComponent

actual fun createTeamlyticsKStorage() = createKStorage<Uuid, TeamlyticsEntity>("teamlytics")

actual fun createTeamlyticsPreviewKStorage() = createKStorage<Uuid, TeamlyticsPreviewEntity>("teamlytics-previews")

actual suspend fun downloadToFile(fileName: String, bytes: ByteArray): Boolean {
    val file = FileKit.openFileSaver(fileName) ?: return false
    file.write(bytes)
    return true
}

private inline fun <reified ID: @Serializable Any, reified T: @Serializable Identifiable<ID>> createKStorage(resourceName: String): KStorage<ID, T> {
    return object : KoinComponent {
    }.let { koinComponent ->
        KStorageImpl(
            listStoreOf(
                file = TODO("Implement Ios KStorage"),
                enableCache = false
            )
        ) { id ->
            storeOf(
                file = TODO("Implement Ios KStorage"),
                enableCache = false,
                version = 0
            )
        }
    }
}
